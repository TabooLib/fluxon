package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.DefaultBytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.*;

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
    private final int rootLocalVariableCount;

    public MainClassEmitter(String className, String superClassName, String fileName, String source, List<Statement> statements, List<Definition> definitions, BytecodeGenerator generator, ClassLoader classLoader) {
        super(className, superClassName, classLoader);
        this.statements = statements;
        this.definitions = definitions;
        this.generator = generator;
        this.fileName = fileName;
        this.source = source;
        this.rootLocalVariableCount = generator.getRootLocalVariableCount();
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
        // 为预解析的扩展函数声明静态字段
        emitResolvedExtensionFunctionsField(ctx);
        // 生成静态初始化块
        emitStaticInit(ownedMainLambdas, ctx);
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
        // 初始化根层级局部变量（_ 前缀变量）
        if (rootLocalVariableCount > 0) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(rootLocalVariableCount);
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "initializeRootLocalVariables", "(I)V", false);
        }
        // 设置 CodeContext
        ctx.allocateLocalVar(Type.OBJECT);
        ctx.allocateLocalVar(Type.OBJECT);
        ctx.setEnvironmentLocalSlot(1);
        ctx.setExpectedReturnType(Object.class);
        // 设置类型分析器（如果可用）
        TypeAnalyzer typeAnalyzer = null;
        if (generator instanceof DefaultBytecodeGenerator) {
            typeAnalyzer = ((DefaultBytecodeGenerator) generator).getTypeAnalyzer();
            ctx.setTypeAnalyzer(typeAnalyzer);
        }
        // 设置 variableTypes 数组（用于 destructure 的类型感知赋值）
        emitVariableTypesInit(mv, typeAnalyzer);
        // 获取 FunctionContextPool 并存入局部变量（避免重复 ThreadLocal.get()）
        mv.visitMethodInsn(INVOKESTATIC, FunctionContextPool.TYPE.getPath(), "local", "()" + FunctionContextPool.TYPE.getDescriptor(), false);
        int poolSlot = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, poolSlot);
        ctx.setPoolLocalSlot(poolSlot);
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
            Instructions.emitLineNumber(statements.get(i), mv);
            last = generator.generateStatementBytecode(statements.get(i), ctx, mv);
            if (i < statementsSize - 1 && last != VOID) {
                mv.visitInsn((last == J || last == D) ? POP2 : POP);
            }
        }
        if (last == null || last == VOID) {
            mv.visitInsn(ACONST_NULL);
        } else if (last.isPrimitive()) {
            Instructions.emitBoxing(mv, last);
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
        Instructions.loadEnvironment(mv, ctx);
        mv.visitInsn(DUP);
        mv.visitLdcInsn(funcDef.getName());
        String functionClassName = className + funcDef.getName();
        mv.visitFieldInsn(GETSTATIC, className, funcDef.getName(), "L" + functionClassName + ";");
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "defineRootFunction", "(" + STRING + Function.TYPE + ")V", false);
    }

    /**
     * 生成设置 variableTypes 数组的代码
     * 只有当存在原始类型变量时才生成
     */
    private void emitVariableTypesInit(MethodVisitor mv, TypeAnalyzer typeAnalyzer) {
        if (typeAnalyzer == null) return;
        java.util.Map<Integer, Type> varTypes = typeAnalyzer.getVariableTypes();
        if (varTypes.isEmpty()) return;
        // 检查是否有原始类型变量
        boolean hasPrimitive = false;
        int maxPos = 0;
        for (java.util.Map.Entry<Integer, Type> entry : varTypes.entrySet()) {
            if (entry.getValue().isPrimitive()) {
                hasPrimitive = true;
            }
            maxPos = Math.max(maxPos, entry.getKey());
        }
        if (!hasPrimitive) return;
        // 创建 Type[] 数组
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitLdcInsn(maxPos + 1);
        mv.visitTypeInsn(ANEWARRAY, Type.SELF.getPath());
        // 只设置原始类型的位置
        for (java.util.Map.Entry<Integer, Type> entry : varTypes.entrySet()) {
            Type type = entry.getValue();
            if (!type.isPrimitive()) continue;
            mv.visitInsn(DUP);
            mv.visitLdcInsn(entry.getKey());
            // 加载对应的 Type 常量
            String fieldName = getTypeFieldName(type);
            if (fieldName != null) {
                mv.visitFieldInsn(GETSTATIC, Type.SELF.getPath(), fieldName, Type.SELF.getDescriptor());
            } else {
                mv.visitInsn(ACONST_NULL);
            }
            mv.visitInsn(AASTORE);
        }
        // this.variableTypes = types
        mv.visitFieldInsn(PUTFIELD, className, "variableTypes", "[" + Type.SELF.getDescriptor());
    }

    /**
     * 获取 Type 常量对应的字段名
     */
    private String getTypeFieldName(Type type) {
        if (type == Type.I) return "I";
        if (type == Type.J) return "J";
        if (type == Type.F) return "F";
        if (type == Type.D) return "D";
        if (type == Type.Z) return "Z";
        return null;
    }

    /**
     * 生成主类的静态初始化块
     */
    private void emitStaticInit(List<LambdaFunctionDefinition> ownedLambdas, CodeContext ctx) {
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
        // 初始化预解析的扩展函数数组
        emitResolvedExtensionFunctionsInit(mv, ctx, className);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
