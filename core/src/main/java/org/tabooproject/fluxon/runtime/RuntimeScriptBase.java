package org.tabooproject.fluxon.runtime;

/**
 * 运行时脚本的基类
 */
public abstract class RuntimeScriptBase {

    protected Environment environment;

    /**
     * 执行函数
     */
    abstract public Object eval(Environment env);

    // 设置运行时变量
    public void setVariable(String name, Object value) {
        environment.assign(name, value);
    }

    // 获取运行时变量
    public Object getVariable(String name) {
        return environment.get(name);
    }

    // 调用运行时函数
    public Object callFunction(String name, Object[] args) {
        return environment.getFunction(name).call(args);
    }

    // 获取运行时环境
    public Environment getEnvironment() {
        return environment;
    }
}