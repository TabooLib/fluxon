package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;

import java.util.*;

import static org.objectweb.asm.Opcodes.POP;

/**
 * 默认字节码生成器实现
 */
public class DefaultBytecodeGenerator implements BytecodeGenerator {

    // 存储脚本主体语句
    private final List<Statement> statements = new ArrayList<>();

    @Override
    public Type generateExpressionBytecode(Expression expr, CodeContext ctx, MethodVisitor mv) {
        ExpressionEvaluator<Expression> evaluator = ctx.getExpression(expr.getExpressionType());
        if (evaluator != null) {
            return evaluator.generateBytecode(expr, ctx, mv);
        } else {
            throw new RuntimeException("No evaluator found for expression type: " + expr.getExpressionType());
        }
    }

    @Override
    public Type generateStatementBytecode(Statement stmt, CodeContext ctx, MethodVisitor mv) {
        StatementEvaluator<Statement> evaluator = ctx.getStatement(stmt.getStatementType());
        if (evaluator != null) {
            return evaluator.generateBytecode(stmt, ctx, mv);
        } else {
            throw new RuntimeException("No evaluator found for statement type: " + stmt.getStatementType());
        }
    }

    @Override
    public void addScriptBody(Statement... statements) {
        Collections.addAll(this.statements, statements);
    }

    @Override
    public void addScriptDefinition(Definition... definitions) {
    }

    @Override
    public byte[] generateClassBytecode(String className) {
        return generateClassBytecode(className, "org/tabooproject/fluxon/runtime/RuntimeScriptBase");
    }

    @Override
    public byte[] generateClassBytecode(String className, String superClassName) {
        CodeContext ctx = new CodeContext(className, superClassName);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        // 生成类，继承 RuntimeScriptBase
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, superClassName, null);

        // 生成空的构造函数
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassName, "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // 生成 eval 函数
        generateEvalMethod(cw, ctx);

        // 生成结束
        cw.visitEnd();
        return cw.toByteArray();
    }

    private void generateEvalMethod(ClassWriter cw, CodeContext ctx) {
        // 继承 Object eval(Environment env) 函数
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "eval", "(" + Environment.TYPE + ")" + Type.OBJECT, null, null);
        mv.visitCode();

        // 设置 environment 参数
        mv.visitVarInsn(Opcodes.ALOAD, 0);  // 加载 this
        mv.visitVarInsn(Opcodes.ALOAD, 1);  // 加载 environment 参数
        mv.visitFieldInsn(Opcodes.PUTFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());

        // 生成脚本主体代码
        Type last = null;
        for (int i = 0, statementsSize = statements.size(); i < statementsSize; i++) {
            last = generateStatementBytecode(statements.get(i), ctx, mv);
            // 如果不是最后一条语句，并且有返回值，则丢弃它
            if (i < statementsSize - 1 && last != Type.VOID) {
                mv.visitInsn(POP);
            }
        }
        // 如果最后一个表达式是 void 类型，则压入 null
        if (last == Type.VOID) {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(9, ctx.getLocalVarIndex() + 1);
        mv.visitEnd();
    }
}