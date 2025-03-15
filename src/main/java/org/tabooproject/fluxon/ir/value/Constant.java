package org.tabooproject.fluxon.ir.value;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRVisitor;

/**
 * 常量值
 * 表示编译时已知的常量值
 */
public abstract class Constant extends AbstractValue {
    
    /**
     * 创建常量值
     * 
     * @param type 值类型
     * @param name 值名称
     */
    protected Constant(IRType type, String name) {
        super(type, name);
    }
    
    @Override
    public boolean isConstant() {
        return true;
    }
    
    /**
     * 获取常量值
     * 
     * @return 常量值
     */
    public abstract Object getValue();
}