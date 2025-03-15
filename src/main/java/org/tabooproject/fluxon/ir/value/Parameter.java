package org.tabooproject.fluxon.ir.value;

import org.tabooproject.fluxon.ir.IRFunction;
import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRVisitor;

/**
 * 参数
 * 表示函数参数
 */
public class Parameter extends AbstractValue {
    private final IRFunction function; // 所属函数
    private final int index; // 参数索引
    
    /**
     * 创建参数
     * 
     * @param function 所属函数
     * @param name 参数名
     * @param type 参数类型
     * @param index 参数索引
     */
    public Parameter(IRFunction function, String name, IRType type, int index) {
        super(type, name);
        this.function = function;
        this.index = index;
    }
    
    /**
     * 获取所属函数
     * 
     * @return 所属函数
     */
    public IRFunction getFunction() {
        return function;
    }
    
    /**
     * 获取参数索引
     * 
     * @return 参数索引
     */
    public int getIndex() {
        return index;
    }
    
    @Override
    public boolean isParameter() {
        return true;
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitParameter(this);
    }
    
    @Override
    public String toString() {
        return "%" + name + ": " + type;
    }
}