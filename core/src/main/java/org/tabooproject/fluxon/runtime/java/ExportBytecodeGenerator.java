package org.tabooproject.fluxon.runtime.java;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassWriter;
import org.tabooproject.fluxon.util.StringUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.objectweb.asm.Opcodes.*;

/**
 * Export 字节码生成器
 * 为带有 @Export 注解的方法生成高性能的直接调用器，替代 method.invoke
 *
 * <h3>生成内容说明</h3>
 * <p>
 * 本生成器会为每个包含 @Export 方法的类生成一个继承自 ClassBridge 的字节码类，例如：
 *
 * <pre>{@code
 * // 原始类
 * public class Base64Object {
 *     @Export
 *     public String encode(String input) { ... }
 *
 *     @Export
 *     public String getCharset() { ... }  // 将注册为 "charset"
 * }
 *
 * // 生成的字节码类（伪代码表示）
 * public class ClassBridge123 extends ClassBridge {
 *     public ClassBridge123(String[] supportedMethods) {
 *         super(supportedMethods); // 传入 ["encode", "charset"]
 *     }
 *
 *     public Object invoke(String methodName, Object instance, Object... args) throws Exception {
 *         Base64Object target = (Base64Object) instance;
 *
 *         if ("encode".equals(methodName)) {
 *             // 安全获取第一个参数（必需）
 *             String arg0 = (String) args[0];
 *             return target.encode(arg0);  // 直接调用原方法
 *         }
 *
 *         if ("charset".equals(methodName)) {
 *             return target.getCharset();  // 调用原方法 getCharset()
 *         }
 *
 *         throw new IllegalArgumentException("Unknown method: " + methodName);
 *     }
 * }
 * }</pre>
 */
public class ExportBytecodeGenerator {

    // 生成的类计数器
    private static final AtomicLong classCounter = new AtomicLong(0);

    // 生成的类桥接器缓存 - 按类型缓存
    private static final ConcurrentHashMap<Class<?>, ClassBridge> generatedBridges = new ConcurrentHashMap<>();

    // 动态类加载器
    private static final FluxonClassLoader fluxonClassLoader = new FluxonClassLoader();

    /**
     * 为指定的类生成优化的类桥接器
     *
     * @param targetClass 要生成桥接器的目标类
     * @return 优化的类桥接器
     * @throws RuntimeException 如果生成失败
     */
    public static ClassBridge generateClassBridge(Class<?> targetClass, Method[] exportMethods, ClassLoader classLoader) {
        // 检查缓存
        ClassBridge cached = generatedBridges.get(targetClass);
        if (cached != null) {
            return cached;
        }
        try {
            // 生成新的类桥接器
            ClassBridge bridge = createOptimizedClassBridge(targetClass, exportMethods, classLoader);
            generatedBridges.put(targetClass, bridge);
            return bridge;
        } catch (Exception e) {
            throw new RuntimeException("为类 " + targetClass.getName() + " 生成字节码桥接器失败", e);
        }
    }

    /**
     * 创建优化的类桥接器
     */
    @SuppressWarnings("unchecked")
    private static ClassBridge createOptimizedClassBridge(Class<?> targetClass, Method[] exportMethods, ClassLoader classLoader) throws Exception {
        String className = ClassBridge.TYPE.getPath() + classCounter.incrementAndGet();

        FluxonClassWriter cw = new FluxonClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, classLoader);

        // 类定义 - 继承 ClassBridge 抽象类
        cw.visit(V1_8, ACC_PUBLIC | ACC_SUPER, className, null, ClassBridge.TYPE.getPath(), null);

        // 生成构造函数，调用父类构造函数传入支持的方法列表
        generateBridgeConstructor(cw, className, exportMethods);

        // 生成 invoke 方法 - 基于方法名分发
        generateBridgeInvokeMethod(cw, className, targetClass, exportMethods);

        cw.visitEnd();

        // 加载生成的类并创建实例
        byte[] bytecode = cw.toByteArray();
        Class<? extends ClassBridge> bridgeClass = (Class<? extends ClassBridge>) fluxonClassLoader.defineClass(className.replace('/', '.'), bytecode);

        // 输出测试文件
        // Files.write(new File("out" + classCounter.get() + ".class").toPath(), bytecode);

        // 创建方法名数组（使用转换后的方法名）
        String[] methodNames = Arrays.stream(exportMethods).map(method -> StringUtils.transformMethodName(method.getName())).toArray(String[]::new);

        return bridgeClass.getDeclaredConstructor(String[].class).newInstance((Object) methodNames);
    }

    /**
     * 生成桥接器构造函数
     */
    private static void generateBridgeConstructor(FluxonClassWriter cw, String className, Method[] exportMethods) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();

        // super(supportedMethods)
        mv.visitVarInsn(ALOAD, 0);  // this
        mv.visitVarInsn(ALOAD, 1);  // supportedMethods 参数
        mv.visitMethodInsn(INVOKESPECIAL, ClassBridge.TYPE.getPath(), "<init>", "([Ljava/lang/String;)V", false);
        mv.visitInsn(RETURN);

        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    /**
     * 生成桥接器的 invoke 方法
     * 基于方法名分发到具体的方法调用
     */
    private static void generateBridgeInvokeMethod(FluxonClassWriter cw, String className, Class<?> targetClass, Method[] exportMethods) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invoke", "(Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", null, new String[]{"java/lang/Exception"});
        mv.visitCode();

        if (exportMethods.length == 0) {
            // 没有方法时直接抛出异常
            mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
            mv.visitInsn(DUP);
            mv.visitLdcInsn("No exported methods available");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
        } else {
            // 生成方法名的 switch-case 分发
            generateMethodDispatch(mv, targetClass, exportMethods);
        }

        mv.visitMaxs(10, 4);
        mv.visitEnd();
    }

    /**
     * 生成 switch 语句进行字符串分发
     * 使用哈希码优化的方式，比 if-else 链更高效
     */
    private static void generateSwitchDispatch(MethodVisitor mv, Map<String, Method> uniqueMethods, Map<String, Label> methodLabels, Label defaultLabel) {
        String[] methodNames = uniqueMethods.keySet().toArray(new String[0]);

        if (methodNames.length <= 3) {
            // 方法数量较少时，仍使用 if-else 链（避免 switch 的额外开销）
            generateIfElseChain(mv, uniqueMethods, methodLabels, defaultLabel);
            return;
        }

        // 按哈希码分组
        Map<Integer, List<String>> hashGroups = new LinkedHashMap<>();
        for (String methodName : methodNames) {
            int hash = methodName.hashCode();
            hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(methodName);
        }

        // 生成 switch 语句
        // switch (methodName.hashCode()) {
        mv.visitVarInsn(ALOAD, 1); // methodName 参数
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);

        // 创建 switch 标签和键值数组，并确保键值按升序排列
        List<Integer> sortedHashes = new ArrayList<>(hashGroups.keySet());
        sortedHashes.sort(Integer::compareTo); // 确保哈希码按升序排列

        Label[] switchLabels = new Label[sortedHashes.size()];
        int[] switchKeys = new int[sortedHashes.size()];

        for (int i = 0; i < sortedHashes.size(); i++) {
            switchLabels[i] = new Label();
            switchKeys[i] = sortedHashes.get(i);
        }

        // 生成 lookupswitch 指令
        mv.visitLookupSwitchInsn(defaultLabel, switchKeys, switchLabels);

        // 为每个哈希码分支生成代码（按排序后的顺序）
        for (int i = 0; i < sortedHashes.size(); i++) {
            mv.visitLabel(switchLabels[i]);

            Integer hash = sortedHashes.get(i);
            List<String> methods = hashGroups.get(hash);

            if (methods.size() == 1) {
                // 只有一个方法，直接比较字符串
                String methodName = methods.get(0);
                mv.visitVarInsn(ALOAD, 1); // methodName 参数
                mv.visitLdcInsn(methodName);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                mv.visitJumpInsn(IFNE, methodLabels.get(methodName));
                mv.visitJumpInsn(GOTO, defaultLabel);
            } else {
                // 多个方法有相同哈希码，需要进一步比较
                for (String methodName : methods) {
                    mv.visitVarInsn(ALOAD, 1); // methodName 参数
                    mv.visitLdcInsn(methodName);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                    mv.visitJumpInsn(IFNE, methodLabels.get(methodName));
                }
                mv.visitJumpInsn(GOTO, defaultLabel);
            }
        }
    }

    /**
     * 生成传统的 if-else 链（用于方法数量较少的情况）
     */
    private static void generateIfElseChain(MethodVisitor mv, Map<String, Method> uniqueMethods, Map<String, Label> methodLabels, Label defaultLabel) {
        for (String methodName : uniqueMethods.keySet()) {
            // 比较方法名: if (methodName.equals("methodName"))
            mv.visitVarInsn(ALOAD, 1);         // methodName 参数
            mv.visitLdcInsn(methodName);       // 目标方法名
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(IFNE, methodLabels.get(methodName)); // 如果相等，跳转到对应方法
        }

        // 没有匹配的方法，跳转到默认处理
        mv.visitJumpInsn(GOTO, defaultLabel);
    }

    /**
     * 生成方法分发逻辑
     */
    private static void generateMethodDispatch(MethodVisitor mv, Class<?> targetClass, Method[] exportMethods) {
        // 检查方法名重复，不允许重载
        Set<String> methodNames = new HashSet<>();
        Map<String, Method> uniqueMethods = new LinkedHashMap<>();

        for (Method method : exportMethods) {
            String transformedName = StringUtils.transformMethodName(method.getName());
            if (!methodNames.add(transformedName)) {
                throw new IllegalArgumentException("类 " + targetClass.getName() + " 中存在重复的 @Export 方法名: " + transformedName + "（原方法名: " + method.getName() + "），不支持方法重载");
            }
            uniqueMethods.put(transformedName, method);
        }

        // 为每个方法名创建标签
        Map<String, Label> methodLabels = new HashMap<>();
        Label defaultLabel = new Label();
        Label endLabel = new Label();

        for (String methodName : uniqueMethods.keySet()) {
            methodLabels.put(methodName, new Label());
        }

        // 使用 switch 语句进行高效的字符串分发
        generateSwitchDispatch(mv, uniqueMethods, methodLabels, defaultLabel);

        // 为每个方法名生成调用代码
        for (String methodName : uniqueMethods.keySet()) {
            mv.visitLabel(methodLabels.get(methodName));
            generateSingleMethodCall(mv, uniqueMethods.get(methodName));
            mv.visitJumpInsn(GOTO, endLabel);
        }

        // 默认处理：抛出异常
        mv.visitLabel(defaultLabel);
        mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
        mv.visitInsn(DUP);
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("Unknown method: ");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(ALOAD, 1); // methodName
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);

        mv.visitLabel(endLabel);
    }

    /**
     * 生成单个方法的调用代码
     */
    private static void generateSingleMethodCall(MethodVisitor mv, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Parameter[] parameters = method.getParameters();

        // 加载实例对象并进行类型转换
        mv.visitVarInsn(ALOAD, 2); // instance 参数
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(method.getDeclaringClass()));

        // 为每个参数安全获取值
        for (int i = 0; i < paramTypes.length; i++) {
            BytecodeUtils.generateSafeParameterAccess(mv, i, paramTypes[i], parameters[i]);
        }

        // 调用目标方法
        String owner = Type.getInternalName(method.getDeclaringClass());
        String methodDesc = Type.getMethodDescriptor(method);
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, method.getName(), methodDesc, false);

        // 处理返回值
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            mv.visitInsn(ACONST_NULL);
        } else if (returnType.isPrimitive()) {
            // 基本类型装箱
            BytecodeUtils.generateBoxing(mv, returnType);
        }

        mv.visitInsn(ARETURN);
    }

    /**
     * 清除缓存
     */
    public static void clearCache() {
        generatedBridges.clear();
    }

    /**
     * 获取缓存统计信息
     */
    public static int getCacheSize() {
        return generatedBridges.size();
    }
}

