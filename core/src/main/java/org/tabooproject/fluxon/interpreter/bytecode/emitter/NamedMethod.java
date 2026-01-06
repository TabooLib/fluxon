package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.objectweb.asm.Label;

import java.lang.reflect.Method;

/**
 * 命名方法封装
 * 包含转换后的方法名、原始 Method 对象和跳转标签
 */
public class NamedMethod {

    private final String name;
    private final Method method;
    private final Label label;

    /**
     * 构造命名方法
     *
     * @param name   转换后的方法名（如 getCharset → charset）
     * @param method 原始 Method 对象
     */
    public NamedMethod(String name, Method method) {
        this.name = name;
        this.method = method;
        this.label = new Label();
    }

    public String getName() {
        return name;
    }

    public Method getMethod() {
        return method;
    }

    public Label getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "NamedMethod{name='" + name + "', method=" + method.getName() + "}";
    }
}
