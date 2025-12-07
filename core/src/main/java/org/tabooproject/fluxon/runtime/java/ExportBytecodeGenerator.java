package org.tabooproject.fluxon.runtime.java;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassWriter;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;
import org.tabooproject.fluxon.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
    private static final FluxonClassLoader fluxonClassLoader = new FluxonClassLoader(ExportBytecodeGenerator.class.getClassLoader());

    private static class NamedMethod {
        public final String name;
        public final Method method;
        public final Label label;

        public NamedMethod(String name, Method method) {
            this.name = name;
            this.method = method;
            this.label = new Label();
        }
    }

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

        // 生成 invoke 方法
        generateBridgeMethodWithDispatch(cw,
                "invoke",
                "(Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                targetClass,
                exportMethods,
                ExportBytecodeGenerator::generateSingleMethodCall);

        // 生成 getParameterTypes 方法
        generateBridgeMethodWithDispatch(cw,
                "getParameterTypes",
                "(Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)[Ljava/lang/Class;",
                targetClass,
                exportMethods,
                ExportBytecodeGenerator::generateSingleMethodParameterTypes);

        cw.visitEnd();

        // 加载生成的类并创建实例
        byte[] bytecode = cw.toByteArray();
        Class<? extends ClassBridge> bridgeClass = (Class<? extends ClassBridge>) fluxonClassLoader.defineClass(className.replace('/', '.'), bytecode);

//        // 输出测试文件
//        Files.write(new File("out" + classCounter.get() + ".class").toPath(), bytecode);

        // 创建方法名数组（使用转换后的方法名）
        String[] methodNames = StringUtils.transformMethodNames(exportMethods);
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
     * 生成带方法分发逻辑的桥接器方法
     *
     * @param cw               ClassWriter
     * @param methodName       方法名
     * @param methodDescriptor 方法描述符
     * @param targetClass      目标类
     * @param exportMethods    导出方法数组
     * @param methodHandler    方法处理器
     */
    private static void generateBridgeMethodWithDispatch(
            FluxonClassWriter cw,
            String methodName,
            String methodDescriptor,
            Class<?> targetClass,
            Method[] exportMethods,
            MethodHandler methodHandler
    ) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, methodDescriptor, null, new String[]{"java/lang/Exception"});
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
            generateDispatchLogic(mv, exportMethods, methodHandler);
        }

        mv.visitMaxs(10, 4);
        mv.visitEnd();
    }

    /**
     * 生成通用的方法分发逻辑
     *
     * @param mv            MethodVisitor
     * @param exportMethods 导出方法数组
     * @param methodHandler 方法处理器，用于生成具体的方法体
     */
    private static void generateDispatchLogic(MethodVisitor mv, Method[] exportMethods, MethodHandler methodHandler) {
        Map<String, List<NamedMethod>> allMethods = buildMethodsMap(exportMethods); // 所有的方法按照名称分组
        List<NamedMethod> uniqueMethods = new LinkedList<>(); // 唯一方法（无重载）
        Map<String, List<NamedMethod>> overrideMethods = new LinkedHashMap<>(); // 重载方法

        // 初始化 uniqueMethods 和 overrideMethods
        for (Map.Entry<String, List<NamedMethod>> entry : allMethods.entrySet()) {
            List<NamedMethod> methodList = entry.getValue();
            boolean hasOverloads = methodList.size() > 1; // 重载判断
            for (NamedMethod method : methodList) {
                if (hasOverloads) {
                    overrideMethods.put(entry.getKey(), methodList);
                } else {
                    uniqueMethods.add(method);
                }
            }
        }

        Label defaultLabel = new Label();
        Label endLabel = new Label();

        // 处理重载方法
        if (!overrideMethods.isEmpty()) {
            generateOverridesChain(mv, overrideMethods, defaultLabel);
        }

        // 方法数量较少时，仍使用 if-else 链（避免 switch 的额外开销）
        if (uniqueMethods.size() <= 3) {
            generateIfElseChain(mv, uniqueMethods, defaultLabel);
        } else {
            // 使用 switch 语句进行高效的字符串分发
            generateSwitchDispatch(mv, uniqueMethods, defaultLabel);
        }

        // 为每个方法名生成具体的处理代码
        for (Collection<NamedMethod> methodList : allMethods.values()) {
            for (NamedMethod namedMethod : methodList) {
                mv.visitLabel(namedMethod.label);
                methodHandler.handle(mv, namedMethod.method);
                mv.visitJumpInsn(GOTO, endLabel);
            }
        }

        // 默认处理：抛出异常
        generateMethodNotFoundError(mv, defaultLabel);

        mv.visitLabel(endLabel);
    }

    /**
     * 生成方法未找到时的异常处理代码
     */
    private static void generateMethodNotFoundError(MethodVisitor mv, Label defaultLabel) {
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
    }

    /**
     * 生成单个方法的参数类型返回代码
     */
    private static void generateSingleMethodParameterTypes(MethodVisitor mv, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();

        // 创建 Class 对象数组: new Class[paramTypes.length]
        mv.visitLdcInsn(paramTypes.length);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");

        for (int i = 0; i < paramTypes.length; i++) {
            mv.visitInsn(DUP);  // 复制数组引用
            mv.visitLdcInsn(i); // 数组索引
            Class<?> paramType = paramTypes[i];
            // 加载 Class 对象
            if (paramType.isPrimitive()) {
                // 基本类型：使用包装类的 TYPE 字段
                mv.visitFieldInsn(GETSTATIC, BytecodeUtils.getWrapperClassName(paramType), "TYPE", "Ljava/lang/Class;");
            } else {
                // 引用类型：使用 LDC 指令直接加载 Class 常量
                mv.visitLdcInsn(Type.getType(paramType));
            }
            // 存入数组
            mv.visitInsn(AASTORE);
        }
        mv.visitInsn(ARETURN);
    }

    /**
     * 生成 switch 语句进行字符串分发
     * 使用哈希码优化的方式，比 if-else 链更高效
     */
    private static void generateSwitchDispatch(MethodVisitor mv, List<NamedMethod> uniqueMethods, Label defaultLabel) {

        // 按哈希码分组
        Map<Integer, List<NamedMethod>> hashGroups = new LinkedHashMap<>();
        for (NamedMethod method : uniqueMethods) {
            int hash = method.name.hashCode();
            hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(method);
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
            List<NamedMethod> methods = hashGroups.get(hash);

            if (methods.size() == 1) {
                // 只有一个方法，直接比较字符串
                NamedMethod method = methods.get(0);
                mv.visitVarInsn(ALOAD, 1); // methodName 参数
                mv.visitLdcInsn(method.name);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                mv.visitJumpInsn(IFNE, method.label);
                mv.visitJumpInsn(GOTO, defaultLabel);
            } else {
                // 多个方法有相同哈希码，需要进一步比较
                for (NamedMethod method : methods) {
                    mv.visitVarInsn(ALOAD, 1); // methodName 参数
                    mv.visitLdcInsn(method.name);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                    mv.visitJumpInsn(IFNE, method.label);
                }
                mv.visitJumpInsn(GOTO, defaultLabel);
            }
        }
    }

    /**
     * 生成传统的 if-else 链（用于方法数量较少的情况）
     */
    private static void generateIfElseChain(MethodVisitor mv, List<NamedMethod> uniqueMethods, Label defaultLabel) {
        for (NamedMethod method : uniqueMethods) {
            // 比较方法名: if (methodName.equals("methodName"))
            mv.visitVarInsn(ALOAD, 1);         // methodName 参数
            mv.visitLdcInsn(method.name);       // 目标方法名
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(IFNE, method.label); // 如果相等，跳转到对应方法
        }
        // 没有匹配的方法，跳转到默认处理
        mv.visitJumpInsn(GOTO, defaultLabel);
    }

    private static void generateOverridesChain(MethodVisitor mv, Map<String, List<NamedMethod>> overrideMethods, Label defaultLabel) {
        for (Map.Entry<String, List<NamedMethod>> entry : overrideMethods.entrySet()) {
            Label nextGroup = new Label();

            // 判断这一组的方法名: if (methodName.equals("methodName"))
            mv.visitVarInsn(ALOAD, 1);         // methodName 参数
            mv.visitLdcInsn(entry.getKey());       // 目标方法名
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(IFEQ, nextGroup); // 如果不相等，跳到下一组

            // 进行参数判断
            List<NamedMethod> methodList = entry.getValue();
            // 排序策略：参数数量升序，相同数量时按类型特异性降序（更具体的类型优先）
            methodList.sort(Comparator
                    .comparingInt((NamedMethod m) -> m.method.getParameterCount())
                    .thenComparing((NamedMethod m) -> -calculateMethodSpecificity(m.method)));
            for (NamedMethod namedMethod : methodList) {
                Label nextMethod = new Label();
                Class<?>[] paramTypes = namedMethod.method.getParameterTypes();

                // 长度检查
                mv.visitVarInsn(ALOAD, 3); // args 数组
                mv.visitInsn(ARRAYLENGTH);
                mv.visitIntInsn(BIPUSH, paramTypes.length);
                mv.visitJumpInsn(IF_ICMPNE, nextMethod);

                // 参数类型兼容性检查（使用 Intrinsics.isCompatibleType 支持基本类型/包装类型互转、继承关系、null 处理）
                for (int i = 0; i < paramTypes.length; i++) {
                    Class<?> paramType = paramTypes[i];

                    // 加载期望的 Class 对象
                    if (paramType.isPrimitive()) {
                        // 基本类型：使用包装类的 TYPE 字段获取原始类型 Class
                        mv.visitFieldInsn(GETSTATIC, BytecodeUtils.getWrapperClassName(paramType), "TYPE", "Ljava/lang/Class;");
                    } else {
                        // 引用类型：使用 LDC 指令直接加载 Class 常量
                        mv.visitLdcInsn(Type.getType(paramType));
                    }

                    // 加载 args[i]
                    mv.visitVarInsn(ALOAD, 3); // args 数组
                    mv.visitIntInsn(BIPUSH, i); // 索引
                    mv.visitInsn(AALOAD); // 获取数组对应索引的元素

                    // 调用 Intrinsics.isCompatibleType(Class<?> expectedType, Object value)
                    mv.visitMethodInsn(INVOKESTATIC,
                            Intrinsics.TYPE.getDescriptor(),
                            "isCompatibleType",
                            "(Ljava/lang/Class;Ljava/lang/Object;)Z",
                            false);
                    mv.visitJumpInsn(IFEQ, nextMethod);
                }
                mv.visitJumpInsn(GOTO, namedMethod.label);
                mv.visitLabel(nextMethod);
            }
            // 未匹配到任何重载，继续检查下一组
            mv.visitLabel(nextGroup);
        }
    }

    /**
     * 计算方法参数类型的总特异性分数
     * 分数越高表示参数类型越具体，在重载解析时应优先匹配
     */
    private static int calculateMethodSpecificity(Method method) {
        int score = 0;
        for (Class<?> paramType : method.getParameterTypes()) {
            score += BytecodeUtils.getTypeSpecificity(paramType);
        }
        return score;
    }

    /**
     * 构建方法映射表 (方法名称 -> 同名的多个方法)
     */
    private static Map<String, List<NamedMethod>> buildMethodsMap(Method[] exportMethods) {
        Map<String, List<NamedMethod>> methodsMap = new LinkedHashMap<>();
        // 获取原始方法名
        Set<String> originalNames = Arrays.stream(exportMethods).map(Method::getName).collect(Collectors.toSet());
        // 输出方法名
        for (Method method : exportMethods) {
            String name = StringUtils.transformMethodName(method.getName());
            // 如果转换后的名称与其他原始方法名冲突，则使用原始名称
            if (originalNames.contains(name)) {
                name = method.getName();
            }
            NamedMethod namedMethod = new NamedMethod(name, method);
            // 添加方法
            methodsMap.computeIfAbsent(name, k -> new LinkedList<>()).add(namedMethod);
        }
        return methodsMap;
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

        // 调用目标方法 - 区分接口和类
        String owner = Type.getInternalName(method.getDeclaringClass());
        String methodDesc = Type.getMethodDescriptor(method);
        boolean isInterface = method.getDeclaringClass().isInterface();

        if (isInterface) {
            // 接口方法调用
            mv.visitMethodInsn(INVOKEINTERFACE, owner, method.getName(), methodDesc, true);
        } else {
            // 类方法调用
            mv.visitMethodInsn(INVOKEVIRTUAL, owner, method.getName(), methodDesc, false);
        }

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

