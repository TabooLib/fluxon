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
        // primitive 不可能为 null，直接返回装箱值
        if (ct.isPrimitive()) {
            interpreter.resultRef = Type.box(interpreter.resultPrimitive, ct);
            return Type.OBJECT;
        }
        if (interpreter.resultRef == null) {
            // condition 为 null，评估 alternative（pass-through，结果已在 single fields）
            return interpreter.evaluate(result.getAlternative());
        }
        // condition 非 null，resultRef 已持有正确值
        return Type.OBJECT;
    }

    @Override
    public Type generateBytecode(ElvisExpression result, CodeContext ctx, MethodVisitor mv) {
        Evaluator<ParseResult> conditionEval = ctx.getEvaluator(result.getCondition());
        Evaluator<ParseResult> alternativeEval = ctx.getEvaluator(result.getAlternative());
        if (conditionEval == null || alternativeEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for operands");
        }

        Label altLabel = new Label();
        Label endLabel = new Label();
        Type conditionType = conditionEval.generateBytecode(result.getCondition(), ctx, mv);
        if (conditionType == Type.VOID) {
            throw new VoidError("Void type is not allowed for elvis condition");
        }
        // 装箱 condition
        if (conditionType.isPrimitive()) {
            boxing(conditionType, mv);
        }

        // 检查是否为 null
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNULL, altLabel);
        // 不为 null，跳转到结束
        mv.visitJumpInsn(GOTO, endLabel);

        // 为 null 时执行替代表达式
        mv.visitLabel(altLabel);
        mv.visitInsn(POP);
        Type alternativeType = alternativeEval.generateBytecode(result.getAlternative(), ctx, mv);
        if (alternativeType == Type.VOID) {
            mv.visitInsn(ACONST_NULL);
        } else if (alternativeType.isPrimitive()) {
            boxing(alternativeType, mv);
        }

        mv.visitLabel(endLabel);
        return Type.OBJECT;
    }

    @Override
    public void analyzeTypes(ElvisExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getCondition());
        analyzer.analyzeNode(result.getAlternative());
    }
}
