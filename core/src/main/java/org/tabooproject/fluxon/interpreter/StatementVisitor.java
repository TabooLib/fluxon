package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.parser.statement.Statement;

/**
 * 语句求值器
 * 处理所有语句类型的求值
 */
public class StatementVisitor extends AbstractVisitor {

    public StatementVisitor(Interpreter interpreter) {
        super(interpreter);
    }

    @Override
    public Object visitStatement(Statement statement) {
        return statement.getStatementType().evaluator.evaluate(interpreter, statement);
    }
}