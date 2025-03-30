package org.tabooproject.fluxon.interpreter.visitors;

import org.tabooproject.fluxon.interpreter.Environment;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.ReturnValue;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definitions.Definition;
import org.tabooproject.fluxon.parser.expressions.Expression;
import org.tabooproject.fluxon.parser.statements.*;

import java.util.List;

/**
 * 语句求值器
 * 处理所有语句类型的求值
 */
public class StatementEvaluator extends AbstractVisitor {
    /**
     * 构造函数
     *
     * @param interpreter 解释器实例
     * @param environment 当前环境
     */
    public StatementEvaluator(Interpreter interpreter, Environment environment) {
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
        // 使用 switch 根据语句类型进行分派
        switch (statement.getStatementType()) {
            case EXPRESSION_STATEMENT:
                return interpreter.evaluate(((ExpressionStatement) statement).getExpression());
            case BLOCK:
                return executeBlock(((Block) statement).getStatements());
            case RETURN:
                return evaluateReturn((ReturnStatement) statement);
            default:
                throw new RuntimeException("Unknown statement type: " + statement.getClass().getName());
        }
    }

    /**
     * 执行代码块
     *
     * @param statements 语句列表
     * @return 最后一个语句的执行结果
     */
    private Object executeBlock(List<ParseResult> statements) {
        // 创建新的环境
        Environment previousEnv = this.environment;
        this.environment = new Environment(previousEnv);

        try {
            Object result = null;
            for (ParseResult statement : statements) {
                result = interpreter.evaluate(statement);
            }
            return result;
        } finally {
            // 恢复环境
            this.environment = previousEnv;
        }
    }

    /**
     * 评估返回语句
     *
     * @param returnStmt 返回语句对象
     * @return 不会有返回值，因为会抛出 ReturnValue 异常
     */
    private Object evaluateReturn(ReturnStatement returnStmt) {
        Object value = null;
        if (returnStmt.getValue() != null) {
            value = interpreter.evaluate(returnStmt.getValue());
        }

        // 通过抛出异常跳出函数执行
        throw new ReturnValue(value);
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