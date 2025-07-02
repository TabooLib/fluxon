package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.GroupingExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;

public class GroupingEvaluator extends ExpressionEvaluator<GroupingExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.GROUPING;
    }

    @Override
    public Object evaluate(Interpreter interpreter, GroupingExpression result) {
        return interpreter.evaluate(result.getExpression());
    }

    @Override
    public Type generateBytecode(GroupingExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取内部表达式的求值器
        Evaluator<ParseResult> eval = ctx.getEvaluator(result.getExpression());
        if (eval == null) {
            throw new RuntimeException("No evaluator found for expression");
        }
        Type type = eval.generateBytecode(result.getExpression(), ctx, mv);
        if (type == Type.VOID) {
            throw new RuntimeException("Void type is not allowed in grouping expression");
        }
        return type;
    }
}
