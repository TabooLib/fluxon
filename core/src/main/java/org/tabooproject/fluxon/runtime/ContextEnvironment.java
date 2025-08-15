package org.tabooproject.fluxon.runtime;

/**
 * 上下文环境
 * 用于上下文调用表达式，自动将未定义的标识符解析为对目标对象的属性访问
 */
public class ContextEnvironment extends Environment {

    public static final Type TYPE = new Type(ContextEnvironment.class);

    private final Object target;

    /**
     * 构造函数
     *
     * @param parent 父环境
     * @param target 目标对象
     */
    public ContextEnvironment(Environment parent, Object target) {
        super(parent, parent.getRoot());
        this.target = target;
        // 将目标对象绑定为 'this'
        defineVariable("this", target);
    }

    public Object getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "ContextEnvironment{" +
                "target=" + target +
                ", parent=" + getParent() +
                '}';
    }
}