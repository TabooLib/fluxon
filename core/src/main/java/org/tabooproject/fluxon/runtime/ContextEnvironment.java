package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.interpreter.error.VariableNotFoundException;

/**
 * 上下文环境
 * 用于上下文调用表达式，自动将未定义的标识符解析为对目标对象的属性访问
 */
public class ContextEnvironment extends Environment {

    private final Object target;

    /**
     * 构造函数
     *
     * @param parent 父环境
     * @param target 目标对象
     */
    public ContextEnvironment(Environment parent, Object target) {
        super(parent);
        this.target = target;
        // 将目标对象绑定为 'this'
        defineVariable("this", target);
    }

    @NotNull
    @Override
    public Object get(String name) {
        try {
            // 首先尝试从父环境获取
            return super.get(name);
        } catch (VariableNotFoundException e) {
            // 如果找不到，尝试作为目标对象的属性访问
            return getTargetProperty(name);
        }
    }

    /**
     * 获取目标对象的属性
     *
     * @param propertyName 属性名
     * @return 属性值
     */
    private Object getTargetProperty(String propertyName) {
        switch (propertyName) {
            case "length":
                return target.toString().length();
            case "toString":
                return target.toString();
            default:
                // 如果不是已知属性，返回属性名作为字符串
                return propertyName;
        }
    }
} 