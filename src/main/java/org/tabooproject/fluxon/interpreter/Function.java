package org.tabooproject.fluxon.interpreter;

/**
 * 函数接口
 * 表示可以被调用的函数
 */
public interface Function {
    
    /**
     * 执行函数
     * 
     * @param args 参数列表
     * @return 返回值
     */
    Object call(Object[] args);
} 