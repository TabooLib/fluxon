package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
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
        // 获取评估器
        Evaluator<ParseResult> conditionEval = ctx.getEvaluator(result.getCondition());
        if (conditionEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for condition expression");
        }
        Evaluator<ParseResult> trueExprEval = ctx.getEvaluator(result.getTrueExpr());
        if (trueExprEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for true expression");
        }
        Evaluator<ParseResult> falseExprEval = ctx.getEvaluator(result.getFalseExpr());
        if (falseExprEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for false expression");
        }

        // 创建局部变量用于存储结果
        int storeId = ctx.allocateLocalVar(Type.OBJECT);
        // 创建标签用于跳转
        Label falseLabel = new Label();
        Label endLabel = new Label();
        
        // 使用标准的条件判断方式
        generateCondition(ctx, mv, result.getCondition(), conditionEval, falseLabel);

        // true 分支代码
        Type trueType = trueExprEval.generateBytecode(result.getTrueExpr(), ctx, mv);
        if (trueType == Type.VOID) {
            mv.visitInsn(ACONST_NULL);
        } else {
            boxing(trueType, mv);
        }
        mv.visitVarInsn(ASTORE, storeId);
        mv.visitJumpInsn(GOTO, endLabel);

        // false 分支标签
        mv.visitLabel(falseLabel);
        // 生成 false 分支的字节码
        Type falseType = falseExprEval.generateBytecode(result.getFalseExpr(), ctx, mv);
        if (falseType == Type.VOID) {
            mv.visitInsn(ACONST_NULL);
        } else {
            boxing(falseType, mv);
        }
        mv.visitVarInsn(ASTORE, storeId);

        // 结束标签
        mv.visitLabel(endLabel);
        // 将结果加载到栈顶
        mv.visitVarInsn(ALOAD, storeId);
        return Type.OBJECT;
    }
}