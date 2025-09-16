package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.*;

/**
 * 默认字节码生成器实现
 */
public class DefaultBytecodeGenerator implements BytecodeGenerator {

    // 存储脚本主体语句
    private final List<Statement> statements = new ArrayList<>();
    // 存储用户函数定义
    private final List<Definition> definitions = new ArrayList<>();

    @Override
    public Type generateExpressionBytecode(Expression expr, CodeContext ctx, MethodVisitor mv) {
        return expr.getExpressionType().evaluator.generateBytecode(expr, ctx, mv);
    }

    @Override
    public Type generateStatementBytecode(Statement stmt, CodeContext ctx, MethodVisitor mv) {
        return stmt.getStatementType().evaluator.generateBytecode(stmt, ctx, mv);
    }

    @Override
    public void addScriptBody(Statement... statements) {
        Collections.addAll(this.statements, statements);
    }

    @Override
    public void addScriptDefinition(Definition... definitions) {
        Collections.addAll(this.definitions, definitions);
    }

    @Override
    public List<byte[]> generateClassBytecode(String className, ClassLoader classLoader) {
        return generateClassBytecode(className, RuntimeScriptBase.TYPE.getPath(), classLoader);
    }

    @Override
    public List<byte[]> generateClassBytecode(String className, String superClassName, ClassLoader classLoader) {
        CodeContext ctx = new CodeContext(className, superClassName);
        ctx.addDefinitions(definitions);
        List<byte[]> byteList = new ArrayList<>();

        // 生成主类
        ClassWriter cw = new FluxonClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, classLoader);
        cw.visit(V1_8, ACC_PUBLIC, className, null, superClassName, null);
        
        // 为每个用户函数生成静态常量字段
        for (Definition definition : definitions) {
            if (definition instanceof FunctionDefinition) {
                FunctionDefinition funcDef = (FunctionDefinition) definition;
                String functionClassName = className + funcDef.getName();
                // 添加静态常量字段: public static final FunctionType functionName = new FunctionType();
                cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, funcDef.getName(), "L" + functionClassName + ";", null, null);
            }
        }

        // 生成静态初始化块
        generateStaticInitializationBlock(cw, ctx);
        
        // 生成空的构造函数
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        
        // 生成 eval 函数
        generateEvalMethod(cw, ctx);
        // 生成 clone 函数
        generateCloneMethod(cw, ctx.getClassName());
        // 主类生成结束
        cw.visitEnd();
        byteList.add(cw.toByteArray());

        // 为每个用户函数生成继承 Function 的内部类
        for (Definition definition : definitions) {
            if (definition instanceof FunctionDefinition) {
                byteList.add(generateFunctionClass((FunctionDefinition) definition, className, classLoader));
            }
        }
        return byteList;
    }

    /**
     * 生成脚本主体函数
     */
    private void generateEvalMethod(ClassWriter cw, CodeContext ctx) {
        // 继承 Object eval(Environment env) 函数
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "eval", "(" + Environment.TYPE + ")" + OBJECT, null, null);
        mv.visitCode();
        // 设置 environment 参数
        mv.visitVarInsn(ALOAD, 0);  // 加载 this
        mv.visitVarInsn(ALOAD, 1);  // 加载 environment 参数
        mv.visitFieldInsn(PUTFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());
        // 注册用户定义的函数到 environment
        for (Definition definition : definitions) {
            if (definition instanceof FunctionDefinition) {
                generatorUserFunctionRegister((FunctionDefinition) definition, mv, ctx);
            }
        }
        // 生成脚本主体代码
        Type last = null;
        for (int i = 0, statementsSize = statements.size(); i < statementsSize; i++) {
            last = generateStatementBytecode(statements.get(i), ctx, mv);
            // 如果不是最后一条语句，并且有返回值，则丢弃它
            if (i < statementsSize - 1 && last != VOID) {
                mv.visitInsn(POP);
            }
        }
        // 如果最后一个表达式是 void 类型，则压入 null
        if (last == null || last == VOID) {
            mv.visitInsn(ACONST_NULL);
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(9, ctx.getLocalVarIndex() + 1);
        mv.visitEnd();
    }

    /**
     * 生成 clone() 方法
     */
    private void generateCloneMethod(ClassWriter cw, String className) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "clone", "()" + RuntimeScriptBase.TYPE, null, null);
        mv.visitCode();
        
        // 创建新实例
        mv.visitTypeInsn(NEW, className);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false);
        
        // 复制 environment 字段
        mv.visitInsn(DUP);          // 复制新实例引用
        mv.visitVarInsn(ALOAD, 0);  // 加载 this
        mv.visitFieldInsn(GETFIELD, className, "environment", Environment.TYPE.getDescriptor());
        mv.visitFieldInsn(PUTFIELD, className, "environment", Environment.TYPE.getDescriptor());
        
        // 返回新实例
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 1);
        mv.visitEnd();
    }

    /**
     * 生成静态初始化块
     */
    private void generateStaticInitializationBlock(ClassWriter cw, CodeContext ctx) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        
        // 为每个用户函数初始化静态常量
        for (Definition definition : definitions) {
            if (definition instanceof FunctionDefinition) {
                FunctionDefinition funcDef = (FunctionDefinition) definition;
                String functionClassName = ctx.getClassName() + funcDef.getName();
                
                // 创建函数实例: new FunctionClassName()
                mv.visitTypeInsn(NEW, functionClassName);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, functionClassName, "<init>", "()V", false);
                // 存储到静态常量字段
                mv.visitFieldInsn(PUTSTATIC, ctx.getClassName(), funcDef.getName(), "L" + functionClassName + ";");
            }
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 0);
        mv.visitEnd();
    }

    /**
     * 注册用户函数到环境中
     * environment.defineFunction(name, staticFunctionInstance)
     */
    private void generatorUserFunctionRegister(FunctionDefinition funcDef, MethodVisitor mv, CodeContext ctx) {
        // 加载 environment
        mv.visitVarInsn(ALOAD, 0);  // this
        mv.visitFieldInsn(GETFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());
        mv.visitInsn(DUP);          // 复制 environment 引用
        // 加载函数名
        mv.visitLdcInsn(funcDef.getName());
        // 获取静态函数实例
        String functionClassName = ctx.getClassName() + funcDef.getName();
        mv.visitFieldInsn(GETSTATIC, ctx.getClassName(), funcDef.getName(), "L" + functionClassName + ";");
        // 调用 defineRootFunction
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "defineRootFunction", "(" + STRING + Function.TYPE + ")V", false);
    }

    /**
     * 生成继承 RuntimeScriptBase 并实现 Function 的独立函数类
     */
    private byte[] generateFunctionClass(FunctionDefinition funcDef, String parentClassName, ClassLoader classLoader) {
        String functionClassName = parentClassName + funcDef.getName();
        ClassWriter cw = new FluxonClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, classLoader);
        // 继承 RuntimeScriptBase 并实现 Function 接口
        cw.visit(V1_8, ACC_PUBLIC, functionClassName, null, RuntimeScriptBase.TYPE.getPath(), new String[]{Function.TYPE.getPath()});
        // 添加 parameters 字段来保存函数参数（static 字段，所有实例共享）
        cw.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "parameters", MAP.getDescriptor(), null, null);

        // 生成构造函数，不需要 environment 参数
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, RuntimeScriptBase.TYPE.getPath(), "<init>", "()V", false);

        // 返回
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // 实现 Function 接口的方法
        generateFunctionInterfaceMethods(funcDef, cw, functionClassName);

        // 生成静态初始化块来初始化 parameters
        generateStaticInitializationBlock(funcDef, cw, functionClassName);

        // 实现 clone 函数
        generateCloneMethod(cw, functionClassName);
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * 生成静态初始化块来初始化 parameters 字段
     */
    private void generateStaticInitializationBlock(FunctionDefinition funcDef, ClassWriter cw, String functionClassName) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        // 将 funcDef.getParameters() 转换为 Map
        BytecodeUtils.generateVariablePositionMap(mv, funcDef.getParameters());
        // 将 Map 存储到 static parameters 字段
        mv.visitFieldInsn(PUTSTATIC, functionClassName, "parameters", MAP.getDescriptor());
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
    }

    /**
     * 生成 Function 接口的实现方法
     */
    private void generateFunctionInterfaceMethods(FunctionDefinition funcDef, ClassWriter cw, String functionClassName) {
        // 实现 getName() 方法
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getName", "()" + STRING, null, null);
        mv.visitCode();
        mv.visitLdcInsn(funcDef.getName());
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // 实现 getParameterCounts() 方法
        mv = cw.visitMethod(ACC_PUBLIC, "getParameterCounts", "()" + LIST, null, null);
        mv.visitCode();
        // 使用 Arrays.asList 创建包含参数数量的列表
        mv.visitIntInsn(BIPUSH, 1);  // 创建大小为 1 的数组
        mv.visitTypeInsn(ANEWARRAY, OBJECT.getPath());
        mv.visitInsn(DUP);
        mv.visitIntInsn(BIPUSH, 0);  // 数组索引 0
        mv.visitIntInsn(BIPUSH, funcDef.getParameters().size());
        mv.visitMethodInsn(INVOKESTATIC, INT.getPath(), "valueOf", "(I)" + INT, false);
        mv.visitInsn(AASTORE);  // 存储到数组中
        mv.visitMethodInsn(INVOKESTATIC, ARRAYS.getPath(), "asList", "([" + OBJECT + ")" + LIST, false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4, 1);
        mv.visitEnd();

        // 实现 getMaxParameterCount() 方法
        mv = cw.visitMethod(ACC_PUBLIC, "getMaxParameterCount", "()I", null, null);
        mv.visitCode();
        mv.visitIntInsn(BIPUSH, funcDef.getParameters().size());
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // 实现 isAsync() 方法
        mv = cw.visitMethod(ACC_PUBLIC, "isAsync", "()Z", null, null);
        mv.visitCode();
        mv.visitInsn(funcDef.isAsync() ? ICONST_1 : ICONST_0);  // 根据函数定义决定
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // 实现 isPrimarySync() 方法
        mv = cw.visitMethod(ACC_PUBLIC, "isPrimarySync", "()Z", null, null);
        mv.visitCode();
        mv.visitInsn(funcDef.isPrimarySync() ? ICONST_1 : ICONST_0);  // 根据函数定义决定
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // 实现 call(FunctionContext) 方法 - 包含函数的实际执行逻辑
        mv = cw.visitMethod(ACC_PUBLIC, "call", "(" + FunctionContext.TYPE + ")" + OBJECT, null, null);
        mv.visitCode();
        // 直接将 Operations.bindFunctionParameters 的结果赋值给 this.environment
        mv.visitVarInsn(ALOAD, 0);  // this（为 PUTFIELD 准备）
        // 准备 Operations.bindFunctionParameters 的参数
        // 从 FunctionContext 获取环境
        mv.visitVarInsn(ALOAD, 1);  // FunctionContext (第一个参数)
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getEnvironment", "()" + Environment.TYPE.getDescriptor(), false);
        // 获取函数参数映射（static 字段）
        mv.visitFieldInsn(GETSTATIC, functionClassName, "parameters", MAP.getDescriptor());
        // 从 FunctionContext 获取参数数组
        mv.visitVarInsn(ALOAD, 1);  // FunctionContext (第一个参数)
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getArguments", "()[" + OBJECT, false);
        // 压入 localVariables
        mv.visitLdcInsn(funcDef.getLocalVariables().size());
        // 调用 Operations.bindFunctionParameters
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "bindFunctionParameters",
                "(" + Environment.TYPE + MAP + "[" + OBJECT + I + ")" + Environment.TYPE,
                false
        );
        mv.visitFieldInsn(PUTFIELD, functionClassName, "environment", Environment.TYPE.getDescriptor());

        // 创建代码上下文并生成函数体字节码
        CodeContext funcCtx = new CodeContext(functionClassName, RuntimeScriptBase.TYPE.getPath());
        // 生成函数体字节码
        Type returnType;
        if (funcDef.getBody() instanceof Statement) {
            returnType = generateStatementBytecode((Statement) funcDef.getBody(), funcCtx, mv);
        } else {
            returnType = generateExpressionBytecode((Expression) funcDef.getBody(), funcCtx, mv);
        }
        // 如果函数体返回 void，则返回 null
        if (returnType == VOID) {
            mv.visitInsn(ACONST_NULL);
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(9, funcCtx.getLocalVarIndex() + 2);
        mv.visitEnd();
    }

    @Override
    public List<Statement> getStatements() {
        return statements;
    }

    @Override
    public List<Definition> getDefinitions() {
        return definitions;
    }

    private static final Type MAP = new Type(Map.class);
    private static final Type LIST = new Type(List.class);
    private static final Type ARRAYS = new Type(Arrays.class);
    private static final Type COLLECTIONS = new Type(Collections.class);
    private static final Type COLLECTION = new Type(Collection.class);

}