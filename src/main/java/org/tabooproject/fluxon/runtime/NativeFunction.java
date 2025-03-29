package org.tabooproject.fluxon.runtime;

import org.tabooproject.fluxon.interpreter.Function;

/**
 * 原生函数类
 * 表示由Java实现的内置函数
 */
public class NativeFunction implements Function {
    
    private final NativeCallable callable;
    
    public NativeFunction(NativeCallable callable) {
        this.callable = callable;
    }
    
    @Override
    public Object call(Object[] args) {
        return callable.call(args);
    }
    
    /**
     * 原生函数接口
     */
    @FunctionalInterface
    public interface NativeCallable {
        /**
         * 调用原生函数
         *
         * @param args 参数列表
         * @return 返回值
         */
        Object call(Object[] args);
    }
} 