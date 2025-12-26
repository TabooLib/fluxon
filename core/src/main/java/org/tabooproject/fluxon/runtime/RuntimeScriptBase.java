package org.tabooproject.fluxon.runtime;

import org.tabooproject.fluxon.parser.SourceExcerpt;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;

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
        Function function = environment.getFunction(name);
        FunctionContextPool pool = FunctionContextPool.local();
        FunctionContext<?> context = pool.borrow(function, target, args, environment);
        try {
            return function.call(context);
        } finally {
            pool.release(context);
        }
    }

    // 获取运行时环境
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * 为运行时错误添加源代码信息
     */
    public static FluxonRuntimeError attachRuntimeError(FluxonRuntimeError ex, String source, String filename, String className) {
        if (ex.getSourceExcerpt() != null) {
            return ex;
        }
        String normalizedClassName = className == null ? null : className.replace('/', '.');
        for (StackTraceElement element : ex.getStackTrace()) {
            if (element.getLineNumber() <= 0) {
                continue;
            }
            if (element.getClassName().equals(normalizedClassName)) {
                SourceExcerpt excerpt = SourceExcerpt.from(filename, source, element.getLineNumber(), 1);
                if (excerpt != null) {
                    ex.attachSource(excerpt);
                }
                break;
            }
        }
        return ex;
    }
}
