package org.tabooproject.fluxon.interpreter.visitors;

import org.tabooproject.fluxon.interpreter.Environment;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definitions.Definition;
import org.tabooproject.fluxon.parser.expressions.Expression;
import org.tabooproject.fluxon.parser.statements.Statement;

/**
 * 抽象访问者类
 * 提供访问者模式的基础实现
 */
public abstract class AbstractVisitor implements Visitor {
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
    
    /**
     * 评估布尔值，判断对象是否为真
     *
     * @param value 要判断的对象
     * @return 布尔值结果
     */
    protected boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        if (value instanceof String) return !((String) value).isEmpty();
        return true;
    }
    
    /**
     * 判断两个对象是否相等
     *
     * @param a 第一个对象
     * @param b 第二个对象
     * @return 是否相等
     */
    protected boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }
}