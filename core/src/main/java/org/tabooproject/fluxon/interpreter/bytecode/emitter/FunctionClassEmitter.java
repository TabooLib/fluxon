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
 * 函数类生成器
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
    private final String fileName;
    private final String source;

    /**
     * 构造函数类生成器
     *
     * @param funcDef         函数定义（可以是普通函数或 Lambda）
     * @param parentClassName 父类名（主类名或所属函数类名）
     * @param fileName        源文件名
     * @param source          源代码
     * @param generator       字节码生成器（用于委托表达式/语句生成）
     * @param classLoader     类加载器
     */
    public FunctionClassEmitter(FunctionDefinition funcDef, String parentClassName, String fileName, String source, BytecodeGenerator generator, ClassLoader classLoader) {
        super(parentClassName + funcDef.getName(), RuntimeScriptBase.TYPE.getPath(), new String[]{Function.TYPE.getPath()}, classLoader);
        this.funcDef = funcDef;
        this.parentClassName = parentClassName;
        this.generator = generator;
        this.fileName = fileName;
        this.source = source;
    }

    @Override
    public EmitResult emit() {
        List<LambdaFunctionDefinition> lambdaDefinitions = new ArrayList<>();
        // 类声明
        beginClass(ACC_PUBLIC, fileName);
        emitSourceMetadataFields(source, fileName);
        // 添加 parameters 和 annotations 静态字段
        emitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "parameters", MAP.getDescriptor(), null);
        emitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "annotations", LIST.getDescriptor(), null);
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
        return new EmitResult(endClass(), lambdaDefinitions);
    }

    // ========== Function 接口方法生成 ==========

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

    private void emitGetNameMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getName", "()" + STRING, null, null);
        mv.visitCode();
        mv.visitLdcInsn(funcDef.getName());
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitGetNamespaceMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getNamespace", "()" + STRING, null, null);
        mv.visitCode();
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitGetParameterCountsMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getParameterCounts", "()" + LIST, null, null);
        mv.visitCode();
        mv.visitIntInsn(BIPUSH, 1);
        mv.visitTypeInsn(ANEWARRAY, OBJECT.getPath());
        mv.visitInsn(DUP);
        mv.visitIntInsn(BIPUSH, 0);
        mv.visitIntInsn(BIPUSH, funcDef.getParameters().size());
        mv.visitMethodInsn(INVOKESTATIC, INT.getPath(), "valueOf", "(I)" + INT, false);
        mv.visitInsn(AASTORE);
        mv.visitMethodInsn(INVOKESTATIC, ARRAYS.getPath(), "asList", "([" + OBJECT + ")" + LIST, false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitGetMaxParameterCountMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getMaxParameterCount", "()I", null, null);
        mv.visitCode();
        mv.visitIntInsn(BIPUSH, funcDef.getParameters().size());
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitIsAsyncMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isAsync", "()Z", null, null);
        mv.visitCode();
        mv.visitInsn(funcDef.isAsync() ? ICONST_1 : ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitIsPrimarySyncMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isPrimarySync", "()Z", null, null);
        mv.visitCode();
        mv.visitInsn(funcDef.isPrimarySync() ? ICONST_1 : ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitGetAnnotationsMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getAnnotations", "()" + LIST, null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, className, "annotations", LIST.getDescriptor());
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitCallMethod(List<LambdaFunctionDefinition> lambdaDefinitions) {
        // 生成 Function.call(FunctionContext) 方法
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", "(" + FunctionContext.TYPE + ")" + OBJECT, null, null);
        mv.visitCode();
        // 初始化代码上下文，预留 slot 0 (this) 和 slot 1 (FunctionContext 参数)
        CodeContext funcCtx = new CodeContext(className, RuntimeScriptBase.TYPE.getPath());
        funcCtx.allocateLocalVar(Type.OBJECT);  // slot 0: this
        funcCtx.allocateLocalVar(Type.OBJECT);  // slot 1: FunctionContext
        // 从 FunctionContext 获取 pool 并存入局部变量（避免重复 ThreadLocal.get()）
        mv.visitVarInsn(ALOAD, 1);  // load FunctionContext
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getPool", "()" + FunctionContextPool.TYPE.getDescriptor(), false);
        int poolSlot = funcCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, poolSlot);
        funcCtx.setPoolLocalSlot(poolSlot);
        // 绑定参数到环境
        emitParameterBinding(mv, funcCtx);
        // 生成函数体字节码
        emitFunctionBody(mv, funcCtx);
        mv.visitMaxs(0, funcCtx.getLocalVarIndex() + 1);
        mv.visitEnd();
        // 收集函数体中发现的 Lambda 定义
        lambdaDefinitions.addAll(funcCtx.getLambdaDefinitions());
    }

    private void emitParameterBinding(MethodVisitor mv, CodeContext funcCtx) {
        // 准备调用 Intrinsics.bindFunctionParameters 的参数
        // 参数1: 父环境 (context.getEnvironment())
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getEnvironment", "()" + Environment.TYPE.getDescriptor(), false);
        // 参数2: 参数位置映射表
        mv.visitFieldInsn(GETSTATIC, className, "parameters", MAP.getDescriptor());
        // 参数3: 实参数组 (context.getArguments())
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getArguments", "()[" + OBJECT, false);
        // 参数4: 局部变量数量
        mv.visitLdcInsn(funcDef.getLocalVariables().size());
        // 调用绑定方法，创建新的函数环境
        mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "bindFunctionParameters", "(" + Environment.TYPE + MAP + "[" + OBJECT + I + ")" + Environment.TYPE, false);
        // 复制环境引用：一份存入局部变量，一份设置到实例字段
        mv.visitInsn(DUP);
        int envSlot = funcCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, envSlot);
        // 将环境设置到 this.environment 字段（供闭包访问）
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(SWAP);
        mv.visitFieldInsn(PUTFIELD, className, "environment", Environment.TYPE.getDescriptor());
        funcCtx.setEnvironmentLocalSlot(envSlot);
    }

    private void emitFunctionBody(MethodVisitor mv, CodeContext funcCtx) {
        // 设置 try-catch 块捕获运行时错误
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        mv.visitTryCatchBlock(start, end, handler, FluxonRuntimeError.class.getName().replace('.', '/'));
        mv.visitLabel(start);
        // 根据函数体类型生成字节码
        Type returnType;
        if (funcDef.getBody() instanceof Statement) {
            BytecodeUtils.emitLineNumber(funcDef.getBody(), mv);
            returnType = generator.generateStatementBytecode((Statement) funcDef.getBody(), funcCtx, mv);
        } else {
            BytecodeUtils.emitLineNumber(funcDef.getBody(), mv);
            returnType = generator.generateExpressionBytecode((Expression) funcDef.getBody(), funcCtx, mv);
        }
        // 若无返回值则压入 null
        if (returnType == VOID) {
            mv.visitInsn(ACONST_NULL);
        }
        // 正常返回路径
        mv.visitLabel(end);
        mv.visitInsn(ARETURN);
        // 异常处理：附加源码位置信息后重新抛出
        mv.visitLabel(handler);
        int exceptionSlot = funcCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, exceptionSlot);
        mv.visitVarInsn(ALOAD, exceptionSlot);
        loadSourceMetadata(mv);
        mv.visitLdcInsn(externalName(className));
        mv.visitMethodInsn(INVOKESTATIC, RuntimeScriptBase.TYPE.getPath(), "attachRuntimeError", "(" + FluxonRuntimeError.TYPE + STRING + STRING + STRING + ")" + FluxonRuntimeError.TYPE, false);
        mv.visitInsn(ATHROW);
    }

    private void emitStaticInit(List<LambdaFunctionDefinition> ownedLambdas) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        // 初始化参数位置映射表: Map<String, Integer>
        BytecodeUtils.generateVariablePositionMap(mv, funcDef.getParameters());
        mv.visitFieldInsn(PUTSTATIC, className, "parameters", MAP.getDescriptor());
        // 初始化注解列表
        emitAnnotationsInit(mv);
        // 初始化所有 Lambda 实例字段
        for (LambdaFunctionDefinition lambdaDef : ownedLambdas) {
            emitLambdaInitialization(mv, lambdaDef, className);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitAnnotationsInit(MethodVisitor mv) {
        List<Annotation> annotations = funcDef.getAnnotations();
        if (annotations.isEmpty()) {
            // 无注解时使用空列表
            mv.visitMethodInsn(INVOKESTATIC, COLLECTIONS.getPath(), "emptyList", "()" + LIST, false);
        } else {
            // 创建注解数组并逐个初始化
            mv.visitIntInsn(BIPUSH, annotations.size());
            mv.visitTypeInsn(ANEWARRAY, ANNOTATION.getPath());
            for (int i = 0; i < annotations.size(); i++) {
                Annotation annotation = annotations.get(i);
                mv.visitInsn(DUP);           // 复制数组引用
                mv.visitIntInsn(BIPUSH, i);  // 数组索引
                BytecodeUtils.generateAnnotation(mv, annotation);
                mv.visitInsn(AASTORE);       // 存入数组
            }
            // 转换为不可变列表
            mv.visitMethodInsn(INVOKESTATIC, ARRAYS.getPath(), "asList", "([" + OBJECT + ")" + LIST, false);
        }
        mv.visitFieldInsn(PUTSTATIC, className, "annotations", LIST.getDescriptor());
    }

    public String getParentClassName() {
        return parentClassName;
    }
}
