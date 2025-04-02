package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;

/**
 * 语句求值器
 * 处理所有语句类型的求值
 */
public class StatementVisitor extends AbstractVisitor {

    public StatementVisitor(Interpreter interpreter, Environment environment) {
        super(interpreter, environment);
    }

    @Override
    public Object visitStatement(Statement statement) {
        StatementEvaluator<Statement> evaluator = registry.getStatement(statement.getStatementType());
        if (evaluator != null) {
            return evaluator.evaluate(interpreter, statement);
        }
        throw new RuntimeException("Unknown statement type: " + statement.getClass().getName());
    }
}