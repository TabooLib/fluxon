package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;

import java.util.*;

/**
 * 默认字节码生成器实现
 */
public class DefaultBytecodeGenerator implements BytecodeGenerator {

    // 存储脚本主体语句
    private final List<Statement> statements = new ArrayList<>();

    @Override
    public Type generateExpressionBytecode(Expression expr, CodeContext ctx, MethodVisitor mv) {
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        ExpressionEvaluator<Expression> evaluator = registry.getExpression(expr.getExpressionType());
        if (evaluator != null) {
            return evaluator.generateBytecode(expr, ctx, mv);
        } else {
            throw new RuntimeException("No evaluator found for expression type: " + expr.getExpressionType());
        }
    }

    @Override
    public void generateStatementBytecode(Statement stmt, CodeContext ctx, MethodVisitor mv) {
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        StatementEvaluator<Statement> evaluator = registry.getStatement(stmt.getStatementType());
        if (evaluator != null) {
            evaluator.generateBytecode(stmt, ctx, mv);
        } else {
            throw new RuntimeException("No evaluator found for statement type: " + stmt.getStatementType());
        }
    }

    @Override
    public void addScriptBody(Statement... statements) {
        Collections.addAll(this.statements, statements);
    }

    @Override
    public byte[] generateClassBytecode(String className) {
        return generateClassBytecode(className, "org/tabooproject/fluxon/runtime/RuntimeScriptBase");
    }

    @Override
    public byte[] generateClassBytecode(String className, String superClassName) {
        CodeContext ctx = new CodeContext(className, superClassName);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        // 生成类，继承 RuntimeScriptBase
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, superClassName, null);
        // 生成构造函数
        generateConstructor(cw, ctx, superClassName);
        cw.visitEnd();
        return cw.toByteArray();
    }

    private void generateConstructor(ClassWriter cw, CodeContext ctx, String superClassName) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                "<init>",
                "(" + Environment.TYPE + ")V",
                null,
                null);
        mv.visitCode();

        // 调用父类构造函数
        mv.visitVarInsn(Opcodes.ALOAD, 0);  // this
        mv.visitVarInsn(Opcodes.ALOAD, 1);  // environment参数
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassName, "<init>", "(" + Environment.TYPE + ")V", false);

        // 生成脚本主体代码
        for (Statement stmt : statements) {
            generateStatementBytecode(stmt, ctx, mv);
        }

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(3, 2);
        mv.visitEnd();
    }
}