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
    REPLACE
}
