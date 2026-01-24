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
import org.tabooproject.fluxon.parser.expression.IfExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.isTrue;

public class IfEvaluator extends ExpressionEvaluator<IfExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.IF;
    }

    @Override
    public Type evaluate(Interpreter interpreter, IfExpression result) {
        Type ct = interpreter.evaluate(result.getCondition());
        if (isTrue(interpreter.getResultBoxed(ct))) {
            return interpreter.evaluate(result.getThenBranch());
        } else if (result.getElseBranch() != null) {
            return interpreter.evaluate(result.getElseBranch());
        } else {
            interpreter.resultRef = null;
            return Type.OBJECT;
        }
    }

    @Override
    public Type generateBytecode(IfExpression result, CodeContext ctx, MethodVisitor mv) {
        Evaluator<ParseResult> conditionEval = ctx.getEvaluator(result.getCondition());
        if (conditionEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for condition");
        }
        Evaluator<ParseResult> thenEval = ctx.getEvaluator(result.getThenBranch());
        if (thenEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for then branch");
        }
        Evaluator<ParseResult> elseEval = result.getElseBranch() != null ? ctx.getEvaluator(result.getElseBranch()) : null;

        // 推断统一类型
        TypeAnalyzer analyzer = ctx.getTypeAnalyzer();
        Type unifiedType = Type.OBJECT;
        if (analyzer != null && elseEval != null) {
            Type thenType = thenEval.inferResultType(result.getThenBranch(), analyzer);
            Type elseType = elseEval.inferResultType(result.getElseBranch(), analyzer);
            unifiedType = unifyBranchTypes(thenType, elseType);
        }

        int storeId = ctx.allocateLocalVar(unifiedType);
        Label elseLabel = new Label();
        Label endLabel = new Label();
        generateCondition(ctx, mv, result.getCondition(), conditionEval, elseLabel);

        // then 分支
        Type thenType = thenEval.generateBytecode(result.getThenBranch(), ctx, mv);
        storeBranchResult(thenType, unifiedType, storeId, mv);
        mv.visitJumpInsn(GOTO, endLabel);

        // else 分支
        mv.visitLabel(elseLabel);
        if (elseEval != null) {
            Type elseType = elseEval.generateBytecode(result.getElseBranch(), ctx, mv);
            storeBranchResult(elseType, unifiedType, storeId, mv);
        } else {
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, storeId);
        }

        mv.visitLabel(endLabel);
        mv.visitVarInsn(unifiedType.isPrimitive() ? loadOpcode(unifiedType) : ALOAD, storeId);
        return unifiedType;
    }

    @Override
    public void analyzeTypes(IfExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getCondition());
        analyzer.analyzeNode(result.getThenBranch());
        analyzer.analyzeNode(result.getElseBranch());
    }

    @Override
    public Type inferResultType(IfExpression result, TypeAnalyzer analyzer) {
        return inferBranchType(result.getThenBranch(), result.getElseBranch(), analyzer);
    }
}
