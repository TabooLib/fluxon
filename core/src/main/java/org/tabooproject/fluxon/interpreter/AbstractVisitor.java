package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;

/**
 * 抽象访问者类
 * 提供访问者模式的基础实现
 */
public abstract class AbstractVisitor implements Visitor {

    // 求值器注册表
    protected final EvaluatorRegistry registry = EvaluatorRegistry.getInstance();

    // 解释器实例，用于回调其他方法
    protected final Interpreter interpreter;
    
    // 当前环境，可能会随着作用域的进入和退出而改变
    protected Environment environment;

    /**
     * 构造函数
     *
     * @param interpreter 解释器实例
     * @param environment 当前环境
     */
    public AbstractVisitor(Interpreter interpreter, Environment environment) {
        this.interpreter = interpreter;
        this.environment = environment;
    }
    
    /**
     * 设置当前环境
     *
     * @param environment 新环境
     */
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * 获取当前环境
     *
     * @return 当前环境
     */
    public Environment getEnvironment() {
        return environment;
    }
    
    /**
     * 访问任何解析结果
     *
     * @param result 解析结果
     * @return 评估结果
     */
    @Override
    public Object visit(ParseResult result) {
        switch (result.getType()) {
            case EXPRESSION:
                return visitExpression((Expression) result);
            case STATEMENT:
                return visitStatement((Statement) result);
            case DEFINITION:
                return visitDefinition((Definition) result);
            default:
                throw new RuntimeException("Unknown parse result type: " + result.getType());
        }
    }
}