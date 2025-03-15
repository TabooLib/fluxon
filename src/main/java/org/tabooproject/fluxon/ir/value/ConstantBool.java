package org.tabooproject.fluxon.ir.value;

import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;

/**
 * 常量布尔值
 * 表示布尔常量值
 */
public class ConstantBool extends Constant {
    private final boolean value;
    
    // 预定义的常量
    public static final ConstantBool TRUE = new ConstantBool(true);
    public static final ConstantBool FALSE = new ConstantBool(false);
    
    /**
     * 创建常量布尔值
     * 
     * @param value 布尔值
     */
    private ConstantBool(boolean value) {
        super(IRTypeFactory.getInstance().getBoolType(), value ? "true" : "false");
        this.value = value;
    }
    
    /**
     * 获取常量布尔值
     * 
     * @param value 布尔值
     * @return 常量布尔值
     */
    public static ConstantBool get(boolean value) {
        return value ? TRUE : FALSE;
    }
    
    @Override
    public Boolean getValue() {
        return value;
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitConstantBool(this);
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}