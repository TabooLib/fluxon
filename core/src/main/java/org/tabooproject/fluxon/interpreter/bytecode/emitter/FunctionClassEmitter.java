package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.*;

/**
 * 函数类发射器
 * 生成实现 Function 接口的类（用户函数和 Lambda 共用）
 */
public class FunctionClassEmitter extends ClassEmitter {

    private static final Type MAP = new Type(Map.class);
    private static final Type LIST = new Type(List.class);
    private static final Type ARRAYS = new Type(Arrays.class);
    private static final Type ANNOTATION = new Type(Annotation.class);
    private static final Type COLLECTIONS = new Type(Collections.class);

    private final FunctionDefinition funcDef;
    private final String parentClassName;
    private final BytecodeGenerator generator;

    /**
     * 构造函数类发射器
     *
     * @param funcDef         函数定义（可以是普通函数或 Lambda）
     * @param parentClassName 父类名（主类名或所属函数类名）
     * @param fileName        源文件名
     * @param source          源代码
     * @param generator       字节码生成器（用于委托表达式/语句生成）
     * @param classLoader     类加载器
     */
    public FunctionClassEmitter(
            FunctionDefinition funcDef,
            String parentClassName,
            String fileName,
            String source,
            BytecodeGenerator generator,
            ClassLoader classLoader) {
        super(parentClassName + funcDef.getName(), RuntimeScriptBase.TYPE.getPath(), fileName, source, classLoader);
        this.funcDef = funcDef;
        this.parentClassName = parentClassName;
        this.generator = generator;
    }

    @Override
    public EmitResult emit() {
        List<LambdaFunctionDefinition> lambdaDefinitions = new ArrayList<>();

        // 类声明：继承 RuntimeScriptBase 并实现 Function 接口
        cw.visit(V1_8, ACC_PUBLIC, className, null, superClassName, new String[]{Function.TYPE.getPath()});
        cw.visitSource(fileName, null);
        emitSourceMetadataFields();

        // 添加 parameters 字段（static，所有实例共享）
        cw.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "parameters", MAP.getDescriptor(), null, null);
        // 添加 annotations 字段（static，所有实例共享）
        cw.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "annotations", LIST.getDescriptor(), null, null);

        // 生成构造函数
        emitDefaultConstructor();
        // 实现 Function 接口方法
        emitFunctionInterfaceMethods(lambdaDefinitions);

        // 为此函数类的 lambda 创建静态字段
        List<LambdaFunctionDefinition> ownedLambdas = getOwnedLambdas(className, lambdaDefinitions);
        for (LambdaFunctionDefinition lambdaDef : ownedLambdas) {
            emitLambdaFieldDeclaration(lambdaDef);
        }

        // 生成静态初始化块
        emitStaticInit(ownedLambdas);
        // 生成 clone 方法
        emitCloneMethod();

        cw.visitEnd();
        return new EmitResult(cw.toByteArray(), lambdaDefinitions);
    }

    /**
     * 生成 Function 接口的所有实现方法
     */
    private void emitFunctionInterfaceMethods(List<LambdaFunctionDefinition> lambdaDefinitions) {
        emitGetNameMethod();
        emitGetNamespaceMethod();
        emitGetParameterCountsMethod();
        emitGetMaxParameterCountMethod();
        emitIsAsyncMethod();
        emitIsPrimarySyncMethod();
        emitGetAnnotationsMethod();
        emitCallMethod(lambdaDefinitions);
    }

    /**
     * 生成 getName() 方法
     */
    private void emitGetNameMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getName", "()" + STRING, null, null);
        mv.visitCode();
        mv.visitLdcInsn(funcDef.getName());
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * 生成 getNamespace() 方法
     */
    private void emitGetNamespaceMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getNamespace", "()" + STRING, null, null);
        mv.visitCode();
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * 生成 getParameterCounts() 方法
     */
    private void emitGetParameterCountsMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getParameterCounts", "()" + LIST, null, null);
        mv.visitCode();
        // 创建大小为 1 的数组
        mv.visitIntInsn(BIPUSH, 1);
        mv.visitTypeInsn(ANEWARRAY, OBJECT.getPath());
        mv.visitInsn(DUP);
        mv.visitIntInsn(BIPUSH, 0);
        mv.visitIntInsn(BIPUSH, funcDef.getParameters().size());
        mv.visitMethodInsn(INVOKESTATIC, INT.getPath(), "valueOf", "(I)" + INT, false);
        mv.visitInsn(AASTORE);
        mv.visitMethodInsn(INVOKESTATIC, ARRAYS.getPath(), "asList", "([" + OBJECT + ")" + LIST, false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4, 1);
        mv.visitEnd();
    }

    /**
     * 生成 getMaxParameterCount() 方法
     */
    private void emitGetMaxParameterCountMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getMaxParameterCount", "()I", null, null);
        mv.visitCode();
        mv.visitIntInsn(BIPUSH, funcDef.getParameters().size());
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * 生成 isAsync() 方法
     */
    private void emitIsAsyncMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isAsync", "()Z", null, null);
        mv.visitCode();
        mv.visitInsn(funcDef.isAsync() ? ICONST_1 : ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * 生成 isPrimarySync() 方法
     */
    private void emitIsPrimarySyncMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isPrimarySync", "()Z", null, null);
        mv.visitCode();
        mv.visitInsn(funcDef.isPrimarySync() ? ICONST_1 : ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * 生成 getAnnotations() 方法
     */
    private void emitGetAnnotationsMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getAnnotations", "()" + LIST, null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, className, "annotations", LIST.getDescriptor());
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * 生成 call(FunctionContext) 方法 - 函数的实际执行逻辑
     */
    private void emitCallMethod(List<LambdaFunctionDefinition> lambdaDefinitions) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", "(" + FunctionContext.TYPE + ")" + OBJECT, null, null);
        mv.visitCode();

        // 创建代码上下文
        CodeContext funcCtx = new CodeContext(className, RuntimeScriptBase.TYPE.getPath());
        funcCtx.allocateLocalVar(Type.OBJECT); // slot 0 (this)
        funcCtx.allocateLocalVar(Type.OBJECT); // slot 1 (FunctionContext)

        // 绑定函数参数，创建新环境
        emitParameterBinding(mv, funcCtx);
        // 生成函数体（包含异常处理）
        emitFunctionBody(mv, funcCtx);

        mv.visitMaxs(0, funcCtx.getLocalVarIndex() + 1);
        mv.visitEnd();

        lambdaDefinitions.addAll(funcCtx.getLambdaDefinitions());
    }

    /**
     * 生成参数绑定代码
     */
    private void emitParameterBinding(MethodVisitor mv, CodeContext funcCtx) {
        // 从 FunctionContext 获取环境
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getEnvironment", "()" + Environment.TYPE.getDescriptor(), false);
        // 获取函数参数映射
        mv.visitFieldInsn(GETSTATIC, className, "parameters", MAP.getDescriptor());
        // 从 FunctionContext 获取参数数组
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getArguments", "()[" + OBJECT, false);
        // 压入 localVariables 大小
        mv.visitLdcInsn(funcDef.getLocalVariables().size());
        // 调用 Intrinsics.bindFunctionParameters
        mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "bindFunctionParameters", "(" + Environment.TYPE + MAP + "[" + OBJECT + I + ")" + Environment.TYPE, false);

        // 复制结果，存储到局部变量
        mv.visitInsn(DUP);
        int envSlot = funcCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, envSlot);

        // 将结果赋值给 this.environment（保持兼容性）
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(SWAP);
        mv.visitFieldInsn(PUTFIELD, className, "environment", Environment.TYPE.getDescriptor());

        // 设置使用局部变量
        funcCtx.setEnvironmentLocalSlot(envSlot);
    }

    /**
     * 生成函数体代码（包含异常处理）
     */
    private void emitFunctionBody(MethodVisitor mv, CodeContext funcCtx) {
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        mv.visitTryCatchBlock(start, end, handler, FluxonRuntimeError.class.getName().replace('.', '/'));
        mv.visitLabel(start);

        // 生成函数体字节码
        Type returnType;
        if (funcDef.getBody() instanceof Statement) {
            BytecodeUtils.emitLineNumber(funcDef.getBody(), mv);
            returnType = generator.generateStatementBytecode((Statement) funcDef.getBody(), funcCtx, mv);
        } else {
            BytecodeUtils.emitLineNumber(funcDef.getBody(), mv);
            returnType = generator.generateExpressionBytecode((Expression) funcDef.getBody(), funcCtx, mv);
        }

        // 如果函数体返回 void，则返回 null
        if (returnType == VOID) {
            mv.visitInsn(ACONST_NULL);
        }

        mv.visitLabel(end);
        mv.visitInsn(ARETURN);

        // 异常处理器
        mv.visitLabel(handler);
        int exceptionSlot = funcCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, exceptionSlot);
        mv.visitVarInsn(ALOAD, exceptionSlot);
        loadSourceMetadata(mv);
        mv.visitLdcInsn(externalName(className));
        mv.visitMethodInsn(
                INVOKESTATIC,
                RuntimeScriptBase.TYPE.getPath(),
                "attachRuntimeError",
                "(" + FluxonRuntimeError.TYPE + STRING + STRING + STRING + ")" + FluxonRuntimeError.TYPE,
                false
        );
        mv.visitInsn(ATHROW);
    }

    /**
     * 生成静态初始化块
     */
    private void emitStaticInit(List<LambdaFunctionDefinition> ownedLambdas) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        // 初始化 parameters Map
        BytecodeUtils.generateVariablePositionMap(mv, funcDef.getParameters());
        mv.visitFieldInsn(PUTSTATIC, className, "parameters", MAP.getDescriptor());
        // 初始化 annotations 列表
        emitAnnotationsInit(mv);
        // 初始化当前函数类拥有的 lambda 单例
        for (LambdaFunctionDefinition lambdaDef : ownedLambdas) {
            emitLambdaInitialization(mv, lambdaDef, className);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * 生成注解初始化代码
     */
    private void emitAnnotationsInit(MethodVisitor mv) {
        List<Annotation> annotations = funcDef.getAnnotations();
        if (annotations.isEmpty()) {
            mv.visitMethodInsn(INVOKESTATIC, COLLECTIONS.getPath(), "emptyList", "()" + LIST, false);
        } else {
            // 创建注解数组
            mv.visitIntInsn(BIPUSH, annotations.size());
            mv.visitTypeInsn(ANEWARRAY, ANNOTATION.getPath());
            // 填充注解数组
            for (int i = 0; i < annotations.size(); i++) {
                Annotation annotation = annotations.get(i);
                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, i);
                BytecodeUtils.generateAnnotation(mv, annotation);
                mv.visitInsn(AASTORE);
            }
            // 使用 Arrays.asList 创建列表
            mv.visitMethodInsn(INVOKESTATIC, ARRAYS.getPath(), "asList", "([" + OBJECT + ")" + LIST, false);
        }
        mv.visitFieldInsn(PUTSTATIC, className, "annotations", LIST.getDescriptor());
    }

    public String getParentClassName() {
        return parentClassName;
    }
}
