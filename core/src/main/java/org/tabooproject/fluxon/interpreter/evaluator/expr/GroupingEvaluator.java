package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.GroupingExpression;
import org.tabooproject.fluxon.runtime.Type;

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
            throw new EvaluatorNotFoundError("No evaluator found for expression");
        }
        Type type = eval.generateBytecode(result.getExpression(), ctx, mv);
        if (type == Type.VOID) {
            throw new VoidError("Void type is not allowed in grouping expression");
        }
        return type;
    }
}
