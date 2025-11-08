package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;

public class ExprStmtEvaluator extends StatementEvaluator<ExpressionStatement> {

    @Override
    public StatementType getType() {
        return StatementType.EXPRESSION_STATEMENT;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ExpressionStatement result) {
        return interpreter.evaluate(result.getExpression());
    }

    @Override
    public Type generateBytecode(ExpressionStatement result, CodeContext ctx, MethodVisitor mv) {
        Evaluator<ParseResult> eval = ctx.getEvaluator(result.getExpression());
        if (eval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for expression");
        }
        return eval.generateBytecode(result.getExpression(), ctx, mv);
    }
}
