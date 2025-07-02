package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ElvisExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Type;
import org.objectweb.asm.Label;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.parser.ParseResult;

import static org.objectweb.asm.Opcodes.*;

public class ElvisEvaluator extends ExpressionEvaluator<ElvisExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.ELVIS;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ElvisExpression result) {
        Object object = interpreter.evaluate(result.getCondition());
        if (object == null) {
            return interpreter.evaluate(result.getAlternative());
        }
        return object;
    }

    @Override
    public Type generateBytecode(ElvisExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取条件表达式和替代表达式的求值器
        Evaluator<ParseResult> conditionEval = ctx.getEvaluator(result.getCondition());
        Evaluator<ParseResult> alternativeEval = ctx.getEvaluator(result.getAlternative());
        if (conditionEval == null || alternativeEval == null) {
            throw new RuntimeException("No evaluator found for operands");
        }
        
        // 创建标签用于跳转
        Label endLabel = new Label();
        // 生成条件表达式的字节码
        Type conditionType = conditionEval.generateBytecode(result.getCondition(), ctx, mv);
        if (conditionType == Type.VOID) {
            throw new RuntimeException("Void type is not allowed for elvis condition");
        }

        // 检查条件表达式结果是否为 null
        mv.visitInsn(DUP);                      // 复制栈顶值用于后续使用
        mv.visitJumpInsn(IFNONNULL, endLabel);  // 如果不为 null，跳转到结束

        // 如果为 null，弹出栈顶值并执行替代表达式
        mv.visitInsn(POP);
        Type alternativeType = alternativeEval.generateBytecode(result.getAlternative(), ctx, mv);
        if (alternativeType == Type.VOID) {
            throw new RuntimeException("Void type is not allowed for elvis alternative");
        }
        
        // 结束标签
        mv.visitLabel(endLabel);

        // 返回结果类型
        if (conditionType == alternativeType) {
            return conditionType;
        }
        return Type.OBJECT;
    }
}
