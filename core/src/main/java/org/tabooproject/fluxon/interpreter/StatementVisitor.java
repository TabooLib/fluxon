package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;

/**
 * 语句求值器
 * 处理所有语句类型的求值
 */
public class StatementVisitor extends AbstractVisitor {

    /**
     * 构造函数
     *
     * @param interpreter 解释器实例
     * @param environment 当前环境
     */
    public StatementVisitor(Interpreter interpreter, Environment environment) {
        super(interpreter, environment);
    }

    /**
     * 访问并评估语句
     *
     * @param statement 语句对象
     * @return 执行结果
     */
    @Override
    public Object visitStatement(Statement statement) {
        StatementEvaluator<Statement> evaluator = registry.getStatement(statement.getStatementType());
        if (evaluator != null) {
            return evaluator.evaluate(interpreter, statement);
        }
        throw new RuntimeException("Unknown statement type: " + statement.getClass().getName());
    }

    /**
     * 不支持的其他类型的 visit 方法
     */
    @Override
    public Object visitExpression(Expression expression) {
        throw new UnsupportedOperationException("Statement evaluator does not support evaluating expressions.");
    }

    @Override
    public Object visitDefinition(Definition definition) {
        throw new UnsupportedOperationException("Statement evaluator does not support evaluating definitions.");
    }
} 