package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ElvisExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Type;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.parser.ParseResult;

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
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        Evaluator<ParseResult> conditionEval = registry.getEvaluator(result.getCondition());
        Evaluator<ParseResult> alternativeEval = registry.getEvaluator(result.getAlternative());
        if (conditionEval == null || alternativeEval == null) {
            throw new RuntimeException("No evaluator found for operands");
        }
        
        // 创建标签用于跳转
        Label endLabel = new Label();
        // 生成条件表达式的字节码
        Type conditionType = conditionEval.generateBytecode(result.getCondition(), ctx, mv);

        // 检查条件表达式结果是否为 null
        mv.visitInsn(Opcodes.DUP);                      // 复制栈顶值用于后续使用
        mv.visitJumpInsn(Opcodes.IFNONNULL, endLabel);  // 如果不为 null，跳转到结束

        // 如果为 null，弹出栈顶值并执行替代表达式
        mv.visitInsn(Opcodes.POP);
        Type alternativeType = alternativeEval.generateBytecode(result.getAlternative(), ctx, mv);
        
        // 结束标签
        mv.visitLabel(endLabel);

        // 返回结果类型
        if (conditionType == alternativeType) {
            return conditionType;
        }
        return Type.OBJECT;
    }
}
