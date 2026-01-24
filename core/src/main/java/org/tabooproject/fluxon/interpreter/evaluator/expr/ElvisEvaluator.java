package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ElvisExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;

import static org.objectweb.asm.Opcodes.*;

public class ElvisEvaluator extends ExpressionEvaluator<ElvisExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.ELVIS;
    }

    @Override
    public Type evaluate(Interpreter interpreter, ElvisExpression result) {
        Type ct = interpreter.evaluate(result.getCondition());
        Object object = interpreter.getResultBoxed(ct);
        if (object == null) {
            return interpreter.evaluate(result.getAlternative());
        }
        interpreter.resultRef = object;
        return Type.OBJECT;
    }

    @Override
    public Type generateBytecode(ElvisExpression result, CodeContext ctx, MethodVisitor mv) {
        Evaluator<ParseResult> conditionEval = ctx.getEvaluator(result.getCondition());
        Evaluator<ParseResult> alternativeEval = ctx.getEvaluator(result.getAlternative());
        if (conditionEval == null || alternativeEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for operands");
        }

        Label endLabel = new Label();
        Type conditionType = conditionEval.generateBytecode(result.getCondition(), ctx, mv);
        if (conditionType == Type.VOID) {
            throw new VoidError("Void type is not allowed for elvis condition");
        }
        boxing(conditionType, mv);

        // 检查是否为 null
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, endLabel);

        // 为 null 时执行替代表达式
        mv.visitInsn(POP);
        Type alternativeType = alternativeEval.generateBytecode(result.getAlternative(), ctx, mv);
        if (alternativeType == Type.VOID) {
            mv.visitInsn(ACONST_NULL);
            alternativeType = Type.OBJECT;
        } else {
            boxing(alternativeType, mv);
        }

        mv.visitLabel(endLabel);

        if (conditionType == alternativeType) {
            return conditionType;
        }
        return Type.OBJECT;
    }

    @Override
    public void analyzeTypes(ElvisExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getCondition());
        analyzer.analyzeNode(result.getAlternative());
    }
}
