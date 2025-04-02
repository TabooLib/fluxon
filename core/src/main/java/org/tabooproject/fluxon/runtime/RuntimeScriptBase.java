package org.tabooproject.fluxon.runtime;

/**
 * 运行时脚本的基类
 */
public abstract class RuntimeScriptBase {

    public static final Type TYPE = new Type(RuntimeScriptBase.class);
    
    protected final Environment environment;
    
    public RuntimeScriptBase(Environment environment) {
        this.environment = environment;
    }

    // 设置运行时变量
    protected void setVariable(String name, Object value) {
        environment.defineVariable(name, value);
    }

    // 获取运行时变量
    protected Object getVariable(String name) {
        return environment.get(name);
    }

    // 调用运行时函数
    protected Object callFunction(String name, Object[] args) {
        return environment.getFunction(name).call(args);
    }

    // 获取运行时环境
    protected Environment getEnvironment() {
        return environment;
    }
}