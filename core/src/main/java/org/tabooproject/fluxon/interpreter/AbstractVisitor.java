package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;

/**
 * 抽象访问者类
 * 提供访问者模式的基础实现
 */
public abstract class AbstractVisitor implements Visitor {

    // 解释器实例，用于回调其他方法
    protected final Interpreter interpreter;
    
    /**
     * 构造函数
     *
     * @param interpreter 解释器实例
     * @param environment 当前环境
     */
    public AbstractVisitor(Interpreter interpreter, Environment environment) {
        this.interpreter = interpreter;
    }
    
    /**
     * 设置当前环境
     *
     * @param environment 新环境
     */
    public void setEnvironment(Environment environment) {
        this.interpreter.setEnvironment(environment);
    }
    
    @Override
    public Object visitExpression(Expression expression) {
        throw new UnsupportedOperationException("Not support.");
    }

    @Override
    public Object visitStatement(Statement statement) {
        throw new UnsupportedOperationException("Not support.");
    }

    @Override
    public Object visitDefinition(Definition definition) {
        throw new UnsupportedOperationException("Not support.");
    }
}