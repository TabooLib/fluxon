package org.tabooproject.fluxon.parser;

/**
 * 可调用对象接口
 * 用于编译期函数调用检查
 */
public interface Callable {

    /**
     * 获取参数数量
     */
    int getParameterCount();
}
