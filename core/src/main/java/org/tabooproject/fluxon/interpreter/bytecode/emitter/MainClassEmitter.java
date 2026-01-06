package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.OBJECT;
import static org.tabooproject.fluxon.runtime.Type.STRING;
import static org.tabooproject.fluxon.runtime.Type.VOID;

/**
 * 主脚本类生成器
 * 生成继承 RuntimeScriptBase 的主类
 */
public class MainClassEmitter extends ClassEmitter {

    private final List<Statement> statements;
    private final List<Definition> definitions;
    private final BytecodeGenerator generator;
    private final String fileName;
    private final String source;

    public MainClassEmitter(String className, String superClassName, String fileName, String source, List<Statement> statements, List<Definition> definitions, BytecodeGenerator generator, ClassLoader classLoader) {
        super(className, superClassName, classLoader);
        this.statements = statements;
        this.definitions = definitions;
        this.generator = generator;
        this.fileName = fileName;
        this.source = source;
    }

    @Override
    public EmitResult emit() {
        List<LambdaFunctionDefinition> lambdaDefinitions = new ArrayList<>();
        CodeContext ctx = new CodeContext(className, superClassName);
        // 类声明
        beginClass(ACC_PUBLIC, fileName);
        emitSourceMetadataFields(source, fileName);
        // 为每个用户函数声明静态常量字段
        emitFunctionStaticFields();
        // 生成空的构造函数
        emitDefaultConstructor();
        // 生成 eval 函数（会收集 lambda）
        emitEvalMethod(ctx, lambdaDefinitions);
        // 为当前类拥有的 lambda 创建静态字段（在收集后）
        List<LambdaFunctionDefinition> ownedMainLambdas = getOwnedLambdas(className, lambdaDefinitions);
        for (LambdaFunctionDefinition lambdaDef : ownedMainLambdas) {
            emitLambdaFieldDeclaration(lambdaDef);
        }
        // 生成静态初始化块
        emitStaticInit(ownedMainLambdas);
        // 生成 clone 函数
        emitCloneMethod();
        return new EmitResult(endClass(), lambdaDefinitions, ctx);
    }

    /**
     * 为用户函数声明静态字段
     */
    private void emitFunctionStaticFields() {
        for (Definition definition : definitions) {
            if (definition instanceof FunctionDefinition) {
                FunctionDefinition funcDef = (FunctionDefinition) definition;
                if (!funcDef.isRegisterToRoot()) {
                    continue;
                }
                String functionClassName = className + funcDef.getName();
                emitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, funcDef.getName(), "L" + functionClassName + ";", null);
            }
        }
    }

    /**
     * 生成 eval(Environment) 方法
     */
    private void emitEvalMethod(CodeContext ctx, List<LambdaFunctionDefinition> lambdaDefinitions) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "eval", "(" + Environment.TYPE + ")" + OBJECT, null, null);
        mv.visitCode();
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        mv.visitTryCatchBlock(start, end, handler, FluxonRuntimeError.class.getName().replace('.', '/'));
        mv.visitLabel(start);
        // 设置 environment 参数
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, className, "environment", Environment.TYPE.getDescriptor());
        // 设置 CodeContext
        ctx.allocateLocalVar(Type.OBJECT);
        ctx.allocateLocalVar(Type.OBJECT);
        ctx.setEnvironmentLocalSlot(1);
        // 注册用户定义的函数到 environment
        for (Definition definition : definitions) {
            if (definition instanceof FunctionDefinition) {
                FunctionDefinition funcDef = (FunctionDefinition) definition;
                if (funcDef.isRegisterToRoot()) {
                    emitUserFunctionRegister(funcDef, mv, ctx);
                }
            }
        }
        // 生成脚本主体代码
        Type last = null;
        for (int i = 0, statementsSize = statements.size(); i < statementsSize; i++) {
            BytecodeUtils.emitLineNumber(statements.get(i), mv);
            last = generator.generateStatementBytecode(statements.get(i), ctx, mv);
            if (i < statementsSize - 1 && last != VOID) {
                mv.visitInsn(POP);
            }
        }
        if (last == null || last == VOID) {
            mv.visitInsn(ACONST_NULL);
        }
        mv.visitLabel(end);
        mv.visitInsn(ARETURN);
        // 异常处理器
        mv.visitLabel(handler);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ALOAD, 2);
        loadSourceMetadata(mv);
        mv.visitLdcInsn(externalName(className));
        mv.visitMethodInsn(INVOKESTATIC, RuntimeScriptBase.TYPE.getPath(), "attachRuntimeError", "(" + FluxonRuntimeError.TYPE + STRING + STRING + STRING + ")" + FluxonRuntimeError.TYPE, false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(0, ctx.getLocalVarIndex() + 3);
        mv.visitEnd();
        lambdaDefinitions.addAll(ctx.getLambdaDefinitions());
    }

    /**
     * 注册用户函数到环境
     */
    private void emitUserFunctionRegister(FunctionDefinition funcDef, MethodVisitor mv, CodeContext ctx) {
        BytecodeUtils.loadEnvironment(mv, ctx);
        mv.visitInsn(DUP);
        mv.visitLdcInsn(funcDef.getName());
        String functionClassName = className + funcDef.getName();
        mv.visitFieldInsn(GETSTATIC, className, funcDef.getName(), "L" + functionClassName + ";");
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "defineRootFunction", "(" + STRING + Function.TYPE + ")V", false);
    }

    /**
     * 生成主类的静态初始化块
     */
    private void emitStaticInit(List<LambdaFunctionDefinition> ownedLambdas) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        // 初始化用户函数静态字段
        for (Definition definition : definitions) {
            if (definition instanceof FunctionDefinition) {
                FunctionDefinition funcDef = (FunctionDefinition) definition;
                if (!funcDef.isRegisterToRoot()) {
                    continue;
                }
                String functionClassName = className + funcDef.getName();
                mv.visitTypeInsn(NEW, functionClassName);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, functionClassName, "<init>", "()V", false);
                mv.visitFieldInsn(PUTSTATIC, className, funcDef.getName(), "L" + functionClassName + ";");
            }
        }
        // 初始化 Lambda 静态字段
        for (LambdaFunctionDefinition lambdaDef : ownedLambdas) {
            emitLambdaInitialization(mv, lambdaDef, className);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
