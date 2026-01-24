package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.LogicalExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;

import static org.objectweb.asm.Opcodes.*;

public class LogicalEvaluator extends ExpressionEvaluator<LogicalExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.LOGICAL;
    }

    @Override
    public Type evaluate(Interpreter interpreter, LogicalExpression result) {
        Type lt = interpreter.evaluate(result.getLeft());
        boolean left = interpreter.isResultTrue(lt);
        if (result.getOperator().getType() == TokenType.OR) {
            if (left) {
                interpreter.resultRef = true;
            } else {
                Type rt = interpreter.evaluate(result.getRight());
                interpreter.resultRef = interpreter.isResultTrue(rt);
            }
        } else {
            if (!left) {
                interpreter.resultRef = false;
            } else {
                Type rt = interpreter.evaluate(result.getRight());
                interpreter.resultRef = interpreter.isResultTrue(rt);
            }
        }
        return Type.BOOLEAN;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Type generateBytecode(LogicalExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取左右表达式的求值器
        Evaluator<ParseResult> leftEval = ctx.getEvaluator(result.getLeft());
        Evaluator<ParseResult> rightEval = ctx.getEvaluator(result.getRight());
        if (leftEval == null || rightEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for operands");
        }
        // 判断左侧表达式
        generateCondition(ctx, mv, result.getLeft(), leftEval, null);

        // 创建跳转标签
        Label endLabel = new Label();
        Label trueLabel = new Label();
        Label falseLabel = new Label();

        // 逻辑与
        if (result.getOperator().getType() == TokenType.OR) {
            // 如果左边结果为 != 0 则跳转到 trueLabel 直接返回 1
            mv.visitJumpInsn(IFNE, trueLabel);
            // 判断右侧表达式
            generateCondition(ctx, mv, result.getRight(), rightEval, falseLabel);
            // 对 1 定义标签
            mv.visitLabel(trueLabel);
        } else {
            // 如果左边结果 == 0 则跳转到 falseLabel 直接返回 0
            mv.visitJumpInsn(IFEQ, falseLabel);
            // 判断右侧表达式
            generateCondition(ctx, mv, result.getRight(), rightEval, falseLabel);
        }

        // 成功
        mv.visitInsn(ICONST_1);
        mv.visitJumpInsn(GOTO, endLabel);
        // 失败
        mv.visitLabel(falseLabel);
        mv.visitInsn(ICONST_0);
        // 结束标签
        mv.visitLabel(endLabel);
        return Type.Z;
    }

    @Override
    public void analyzeTypes(LogicalExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getLeft());
        analyzer.analyzeNode(result.getRight());
    }

    @Override
    public Type inferResultType(LogicalExpression result, TypeAnalyzer analyzer) {
        return Type.Z;
    }
}
