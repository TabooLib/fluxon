package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.TernaryExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.isTrue;

/**
 * 三元运算符求值器
 */
public class TernaryEvaluator extends ExpressionEvaluator<TernaryExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.TERNARY;
    }

    @Override
    public Type evaluate(Interpreter interpreter, TernaryExpression result) {
        Type ct = interpreter.evaluate(result.getCondition());
        if (isTrue(interpreter.getResultBoxed(ct))) {
            return interpreter.evaluate(result.getTrueExpr());
        } else {
            return interpreter.evaluate(result.getFalseExpr());
        }
    }

    @Override
    public Type generateBytecode(TernaryExpression result, CodeContext ctx, MethodVisitor mv) {
        Evaluator<ParseResult> conditionEval = ctx.getEvaluator(result.getCondition());
        if (conditionEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for condition");
        }
        Evaluator<ParseResult> trueEval = ctx.getEvaluator(result.getTrueExpr());
        if (trueEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for true branch");
        }
        Evaluator<ParseResult> falseEval = ctx.getEvaluator(result.getFalseExpr());
        if (falseEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for false branch");
        }

        // 推断统一类型
        TypeAnalyzer analyzer = ctx.getTypeAnalyzer();
        Type unifiedType = Type.OBJECT;
        if (analyzer != null) {
            Type trueType = trueEval.inferResultType(result.getTrueExpr(), analyzer);
            Type falseType = falseEval.inferResultType(result.getFalseExpr(), analyzer);
            unifiedType = unifyBranchTypes(trueType, falseType);
        }

        int storeId = ctx.allocateLocalVar(unifiedType);
        Label falseLabel = new Label();
        Label endLabel = new Label();
        generateCondition(ctx, mv, result.getCondition(), conditionEval, falseLabel);

        // true 分支
        Type trueType = trueEval.generateBytecode(result.getTrueExpr(), ctx, mv);
        storeBranchResult(trueType, unifiedType, storeId, mv);
        mv.visitJumpInsn(GOTO, endLabel);

        // false 分支
        mv.visitLabel(falseLabel);
        Type falseType = falseEval.generateBytecode(result.getFalseExpr(), ctx, mv);
        storeBranchResult(falseType, unifiedType, storeId, mv);

        mv.visitLabel(endLabel);
        mv.visitVarInsn(unifiedType.isPrimitive() ? loadOpcode(unifiedType) : ALOAD, storeId);
        return unifiedType;
    }

    @Override
    public void analyzeTypes(TernaryExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getCondition());
        analyzer.analyzeNode(result.getTrueExpr());
        analyzer.analyzeNode(result.getFalseExpr());
    }

    @Override
    public Type inferResultType(TernaryExpression result, TypeAnalyzer analyzer) {
        return inferBranchType(result.getTrueExpr(), result.getFalseExpr(), analyzer);
    }
}