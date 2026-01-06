package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassWriter;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.STRING;

/**
 * 类字节码发射器基类
 * 提供生成 Fluxon 类的公共方法
 */
public abstract class ClassEmitter {

    protected final ClassWriter cw;
    protected final String className;
    protected final String superClassName;
    protected final String fileName;
    protected final String source;

    protected ClassEmitter(String className, String superClassName, String fileName, String source, ClassLoader classLoader) {
        this.className = className;
        this.superClassName = superClassName;
        this.fileName = fileName;
        this.source = source;
        this.cw = new FluxonClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, classLoader);
    }

    /**
     * 生成类字节码
     *
     * @return 发射结果，包含字节码和收集到的 Lambda 定义
     */
    public abstract EmitResult emit();

    /**
     * 生成默认无参构造函数
     */
    protected void emitDefaultConstructor() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * 生成 clone() 方法
     */
    protected void emitCloneMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "clone", "()" + RuntimeScriptBase.TYPE, null, null);
        mv.visitCode();
        // 创建新实例
        mv.visitTypeInsn(NEW, className);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
        // 复制 environment 字段
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "environment", org.tabooproject.fluxon.runtime.Environment.TYPE.getDescriptor());
        mv.visitFieldInsn(PUTFIELD, className, "environment", org.tabooproject.fluxon.runtime.Environment.TYPE.getDescriptor());
        // 返回新实例
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 1);
        mv.visitEnd();
    }

    /**
     * 声明 __source 和 __filename 静态字段
     */
    protected void emitSourceMetadataFields() {
        cw.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "__source", STRING.getDescriptor(), null, source);
        cw.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "__filename", STRING.getDescriptor(), null, fileName);
    }

    /**
     * 加载源码元数据到栈顶（用于错误处理）
     *
     * @param mv 方法访问器
     */
    protected void loadSourceMetadata(MethodVisitor mv) {
        mv.visitFieldInsn(GETSTATIC, className, "__source", STRING.getDescriptor());
        mv.visitFieldInsn(GETSTATIC, className, "__filename", STRING.getDescriptor());
    }

    /**
     * 声明 Lambda 静态字段
     *
     * @param def Lambda 函数定义
     */
    protected void emitLambdaFieldDeclaration(LambdaFunctionDefinition def) {
        String lambdaClassName = def.getOwnerClassName() + def.getName();
        cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, def.getName(), "L" + lambdaClassName + ";", null, null);
    }

    /**
     * 在 clinit 中初始化 Lambda 单例
     *
     * @param mv         方法访问器
     * @param def        Lambda 函数定义
     * @param ownerClass 持有该 Lambda 字段的类名
     */
    protected void emitLambdaInitialization(MethodVisitor mv, LambdaFunctionDefinition def, String ownerClass) {
        String lambdaClassName = def.getOwnerClassName() + def.getName();
        mv.visitTypeInsn(NEW, lambdaClassName);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, lambdaClassName, "<init>", "()V", false);
        mv.visitFieldInsn(PUTSTATIC, ownerClass, def.getName(), "L" + lambdaClassName + ";");
    }

    /**
     * 内部类名转外部类名
     *
     * @param internalName 内部名称（使用 / 分隔）
     * @return 外部名称（使用 . 分隔）
     */
    protected String externalName(String internalName) {
        return internalName.replace('/', '.');
    }

    /**
     * 过滤出属于指定类的 Lambda 定义
     *
     * @param ownerClassName 所属类名
     * @param all            所有 Lambda 定义
     * @return 属于指定类的 Lambda 定义列表
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
}
