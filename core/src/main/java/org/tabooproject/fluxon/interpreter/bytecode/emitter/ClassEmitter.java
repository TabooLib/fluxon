package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassWriter;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.STRING;

/**
 * 类字节码生成器基类
 * 提供生成字节码类的通用方法
 */
@SuppressWarnings("SameParameterValue")
public abstract class ClassEmitter {

    protected final ClassWriter cw;
    protected final String className;
    protected final String superClassName;
    protected final String[] interfaces;

    /**
     * 构造生成器
     *
     * @param className      类名（内部格式，使用 / 分隔）
     * @param superClassName 父类名（内部格式）
     * @param interfaces     实现的接口列表（可为 null）
     * @param classLoader    类加载器
     */
    protected ClassEmitter(String className, String superClassName, String[] interfaces, ClassLoader classLoader) {
        this.className = className;
        this.superClassName = superClassName;
        this.interfaces = interfaces;
        this.cw = new FluxonClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, classLoader);
    }

    /**
     * 便捷构造器（无接口）
     */
    protected ClassEmitter(String className, String superClassName, ClassLoader classLoader) {
        this(className, superClassName, null, classLoader);
    }

    /**
     * 开始类定义
     *
     * @param access 访问修饰符
     */
    protected void beginClass(int access) {
        cw.visit(V1_8, access, className, null, superClassName, interfaces);
    }

    /**
     * 开始类定义（带源文件名）
     *
     * @param access   访问修饰符
     * @param fileName 源文件名
     */
    protected void beginClass(int access, String fileName) {
        cw.visit(V1_8, access, className, null, superClassName, interfaces);
        if (fileName != null) {
            cw.visitSource(fileName, null);
        }
    }

    /**
     * 生成构造函数
     *
     * @param descriptor      构造函数描述符
     * @param superDescriptor 父类构造函数描述符
     * @param bodyGenerator   构造函数体生成器（在 super() 调用之后执行，可为 null）
     */
    protected void emitConstructor(String descriptor, String superDescriptor, Consumer<MethodVisitor> bodyGenerator) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
        mv.visitCode();
        // 调用父类构造函数
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>", superDescriptor, false);
        // 执行构造函数体
        if (bodyGenerator != null) {
            bodyGenerator.accept(mv);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * 生成带参数传递的构造函数
     *
     * @param descriptor      构造函数描述符
     * @param superDescriptor 父类构造函数描述符
     * @param superArgsLoader 加载传递给 super() 的参数（在 ALOAD 0 之后执行）
     */
    protected void emitConstructorWithSuperArgs(String descriptor, String superDescriptor, Consumer<MethodVisitor> superArgsLoader) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
        mv.visitCode();
        // 调用父类构造函数
        mv.visitVarInsn(ALOAD, 0);
        // 加载 super() 参数
        if (superArgsLoader != null) {
            superArgsLoader.accept(mv);
        }
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>", superDescriptor, false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * 生成默认无参构造函数
     */
    protected void emitDefaultConstructor() {
        emitConstructor("()V", "()V", null);
    }

    /**
     * 声明字段
     *
     * @param access     访问修饰符
     * @param name       字段名
     * @param descriptor 字段描述符
     * @param value      初始值（仅对静态常量有效，可为 null）
     */
    protected void emitField(int access, String name, String descriptor, Object value) {
        cw.visitField(access, name, descriptor, null, value);
    }

    /**
     * 声明 __source 和 __filename 静态字段
     */
    protected void emitSourceMetadataFields(String source, String fileName) {
        emitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "__source", STRING.getDescriptor(), source);
        emitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "__filename", STRING.getDescriptor(), fileName);
    }

    /**
     * 加载源码元数据到栈顶（用于错误处理）
     */
    protected void loadSourceMetadata(MethodVisitor mv) {
        mv.visitFieldInsn(GETSTATIC, className, "__source", STRING.getDescriptor());
        mv.visitFieldInsn(GETSTATIC, className, "__filename", STRING.getDescriptor());
    }

    /**
     * 生成 clone() 方法
     */
    protected void emitCloneMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "clone", "()" + RuntimeScriptBase.TYPE, null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, className);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "environment", Environment.TYPE.getDescriptor());
        mv.visitFieldInsn(PUTFIELD, className, "environment", Environment.TYPE.getDescriptor());
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * 声明 Lambda 静态字段
     */
    protected void emitLambdaFieldDeclaration(LambdaFunctionDefinition def) {
        String lambdaClassName = def.getOwnerClassName() + def.getName();
        emitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, def.getName(), "L" + lambdaClassName + ";", null);
    }

    /**
     * 在 clinit 中初始化 Lambda 单例
     */
    protected void emitLambdaInitialization(MethodVisitor mv, LambdaFunctionDefinition def, String ownerClass) {
        String lambdaClassName = def.getOwnerClassName() + def.getName();
        mv.visitTypeInsn(NEW, lambdaClassName);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, lambdaClassName, "<init>", "()V", false);
        mv.visitFieldInsn(PUTSTATIC, ownerClass, def.getName(), "L" + lambdaClassName + ";");
    }

    /**
     * 过滤出属于指定类的 Lambda 定义
     */
    protected static List<LambdaFunctionDefinition> getOwnedLambdas(String ownerClassName, List<LambdaFunctionDefinition> all) {
        List<LambdaFunctionDefinition> owned = new ArrayList<>();
        for (LambdaFunctionDefinition def : all) {
            if (ownerClassName.equals(def.getOwnerClassName())) {
                owned.add(def);
            }
        }
        return owned;
    }

    /**
     * 声明 RESOLVED_EXT_FUNCTIONS 静态字段（如果需要）
     *
     * @param ctx CodeContext 用于检查是否有预解析函数
     */
    protected void emitResolvedExtensionFunctionsField(CodeContext ctx) {
        if (!ctx.getResolvedExtensionFunctions().isEmpty()) {
            emitField(ACC_PUBLIC | ACC_STATIC, "RESOLVED_EXT_FUNCTIONS", "[" + Function.TYPE.getDescriptor(), null);
        }
    }

    /**
     * 在 clinit 中初始化 RESOLVED_EXT_FUNCTIONS 数组
     *
     * @param mv         静态初始化方法 visitor
     * @param ctx        CodeContext 包含预解析函数列表
     * @param ownerClass 静态字段所属类
     */
    protected void emitResolvedExtensionFunctionsInit(MethodVisitor mv, CodeContext ctx, String ownerClass) {
        List<CodeContext.ResolvedExtFuncInfo> functions = ctx.getResolvedExtensionFunctions();
        if (functions.isEmpty()) {
            return;
        }
        // 创建数组: new Function[size]
        mv.visitLdcInsn(functions.size());
        mv.visitTypeInsn(ANEWARRAY, Function.TYPE.getPath());
        // 填充数组元素
        for (int i = 0; i < functions.size(); i++) {
            CodeContext.ResolvedExtFuncInfo info = functions.get(i);
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            // 通过 FluxonRuntime 获取派发表，然后调用 resolve(targetClass)
            emitLoadFunction(mv, info);
            mv.visitInsn(AASTORE);
        }
        // 存入静态字段
        mv.visitFieldInsn(PUTSTATIC, ownerClass, "RESOLVED_EXT_FUNCTIONS", "[" + Function.TYPE.getDescriptor());
    }

    /**
     * 生成加载预解析函数的字节码
     * 通过扩展派发表获取函数引用
     */
    private void emitLoadFunction(MethodVisitor mv, CodeContext.ResolvedExtFuncInfo info) {
        // FluxonRuntime.getInstance().getCachedDispatchTables()[dispatchTableIndex].resolve(targetClass)
        mv.visitMethodInsn(INVOKESTATIC, "org/tabooproject/fluxon/runtime/FluxonRuntime", "getInstance", "()Lorg/tabooproject/fluxon/runtime/FluxonRuntime;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/tabooproject/fluxon/runtime/FluxonRuntime", "getCachedDispatchTables", "()[Lorg/tabooproject/fluxon/runtime/ExtensionDispatchTable;", false);
        mv.visitLdcInsn(info.dispatchTableIndex);
        mv.visitInsn(AALOAD);
        mv.visitLdcInsn(org.objectweb.asm.Type.getType(info.targetClass));
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/tabooproject/fluxon/runtime/ExtensionDispatchTable", "resolve", "(Ljava/lang/Class;)" + Function.TYPE.getDescriptor(), false);
    }

    /**
     * 结束类定义并返回字节码
     *
     * @return 类字节码
     */
    protected byte[] endClass() {
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * 内部类名转外部类名
     *
     * @param internalName 内部名称（使用 / 分隔）
     * @return 外部名称（使用 . 分隔）
     */
    protected static String externalName(String internalName) {
        return internalName.replace('/', '.');
    }

    /**
     * 生成类字节码
     *
     * @return 生成结果
     */
    public abstract EmitResult emit();

    // ========== Getter ==========

    public String getClassName() {
        return className;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public ClassWriter getClassWriter() {
        return cw;
    }
}
