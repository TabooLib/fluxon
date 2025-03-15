package org.tabooproject.fluxon.ir.value;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;

/**
 * 常量浮点数
 * 表示浮点数常量值
 */
public class ConstantFloat extends Constant {
    private final double value;
    private final boolean isDouble; // 是否为双精度浮点数
    
    /**
     * 创建常量浮点数
     * 
     * @param value 浮点数值
     * @param isDouble 是否为双精度浮点数
     */
    public ConstantFloat(double value, boolean isDouble) {
        super(isDouble ? IRTypeFactory.getInstance().getDoubleType() : IRTypeFactory.getInstance().getFloatType(),
                (isDouble ? "double_" : "float_") + value);
        this.value = value;
        this.isDouble = isDouble;
    }
    
    /**
     * 创建单精度常量浮点数
     * 
     * @param value 浮点数值
     * @return 常量浮点数
     */
    public static ConstantFloat getFloat(float value) {
        return new ConstantFloat(value, false);
    }
    
    /**
     * 创建双精度常量浮点数
     * 
     * @param value 浮点数值
     * @return 常量浮点数
     */
    public static ConstantFloat getDouble(double value) {
        return new ConstantFloat(value, true);
    }
    
    @Override
    public Double getValue() {
        return value;
    }
    
    /**
     * 检查是否为双精度浮点数
     * 
     * @return 是否为双精度浮点数
     */
    public boolean isDouble() {
        return isDouble;
    }
    
    /**
     * 获取单精度浮点数值
     * 
     * @return 单精度浮点数值
     */
    public float getFloatValue() {
        return (float) value;
    }
    
    /**
     * 获取双精度浮点数值
     * 
     * @return 双精度浮点数值
     */
    public double getDoubleValue() {
        return value;
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitConstantFloat(this);
    }
    
    @Override
    public String toString() {
        if (isDouble) {
            return String.valueOf(value);
        } else {
            return String.valueOf((float) value) + "f";
        }
    }
}