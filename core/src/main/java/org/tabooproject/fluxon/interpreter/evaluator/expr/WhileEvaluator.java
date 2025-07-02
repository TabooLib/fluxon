package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.BreakException;
import org.tabooproject.fluxon.interpreter.ContinueException;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.WhileExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Operations;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.isTrue;

public class WhileEvaluator extends ExpressionEvaluator<WhileExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.WHILE;
    }

    @Override
    public Object evaluate(Interpreter interpreter, WhileExpression result) {
        Object last = null;
        while (isTrue(interpreter.evaluate(result.getCondition()))) {
            try {
                last = interpreter.evaluate(result.getBody());
            } catch (ContinueException ignored) {
            } catch (BreakException ignored) {
                break;
            }
        }
        return last;
    }

    /*
            whileStart:
            评估条件表达式
            |
            调用 Operations.isTrue 判断条件
            |
            +--> 如果为假，跳到 whileEnd
            |
            执行循环体（丢弃返回值）
            |
            跳回 whileStart
            |
            V
            whileEnd:
     */
    @Override
    public Type generateBytecode(WhileExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取评估器注册表
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        Evaluator<ParseResult> conditionEval = registry.getEvaluator(result.getCondition());
        if (conditionEval == null) {
            throw new RuntimeException("No evaluator found for condition expression");
        }
        Evaluator<ParseResult> bodyEval = registry.getEvaluator(result.getBody());
        if (bodyEval == null) {
            throw new RuntimeException("No evaluator found for body expression");
        }

        // 创建标签用于跳转
        Label whileStart = new Label();
        Label whileEnd = new Label();
        // while 循环开始标签
        mv.visitLabel(whileStart);
        // 评估条件表达式
        generateCondition(ctx, mv, result.getCondition(), conditionEval, whileEnd);

        // 执行循环体
        Type bodyType = bodyEval.generateBytecode(result.getBody(), ctx, mv);
        // 如果循环体有返回值，则丢弃它
        if (bodyType != Type.VOID) {
            mv.visitInsn(POP);
        }
        // 跳回循环开始
        mv.visitJumpInsn(GOTO, whileStart);
        // while 循环结束标签
        mv.visitLabel(whileEnd);
        return Type.VOID;
    }
}
