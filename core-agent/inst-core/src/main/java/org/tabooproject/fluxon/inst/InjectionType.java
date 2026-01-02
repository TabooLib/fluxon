package org.tabooproject.fluxon.inst;

/**
 * 注入类型枚举。
 */
public enum InjectionType {

    /**
     * 在方法入口处注入代码，原始逻辑执行前执行。
     */
    BEFORE,
    
    /**
     * 替换整个方法实现。
     */
    REPLACE,
    
    /**
     * 在方法退出时注入代码（包括正常返回和异常退出）。
     * 回调可以访问返回值（或异常对象）并修改返回值。
     */
    AFTER
}
