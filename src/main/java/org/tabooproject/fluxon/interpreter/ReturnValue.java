package org.tabooproject.fluxon.interpreter;

/**
 * 返回值类
 * 用于处理函数中的return语句
 * 通过抛出异常方式跳出函数执行流程
 */
public class ReturnValue extends RuntimeException {
    
    private final Object value;
    
    public ReturnValue(Object value) {
        super(null, null, false, false); // 禁用堆栈跟踪以提高性能
        this.value = value;
    }
    
    /**
     * 获取返回值
     *
     * @return 返回值
     */
    public Object getValue() {
        return value;
    }
} 