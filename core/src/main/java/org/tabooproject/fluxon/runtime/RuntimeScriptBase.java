package org.tabooproject.fluxon.runtime;

/**
 * 运行时脚本的基类
 */
public abstract class RuntimeScriptBase {

    public static final Type TYPE = new Type(RuntimeScriptBase.class);

    protected Environment environment;

    // 克隆当前脚本
    abstract public RuntimeScriptBase clone();

    /**
     * 执行函数
     */
    public Object eval(Environment env) {
        return null;
    }

    // 设置运行时变量
    public void assign(String name, Object value, int index) {
        environment.assign(name, value, index);
    }

    // 获取运行时变量
    public Object get(String name, int index) {
        return environment.get(name, index);
    }

    // 调用运行时函数
    public Object callFunction(String name, Object target, Object[] args) {
        final FunctionContext<?> context = new FunctionContext<>(target, args, environment);
        return environment.getFunction(name).call(context);
    }

    // 获取运行时环境
    public Environment getEnvironment() {
        return environment;
    }
}