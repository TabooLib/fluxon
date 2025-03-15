package org.tabooproject.fluxon.ir.value;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;

/**
 * 常量整数
 * 表示整数常量值
 */
public class ConstantInt extends Constant {
    private final long value;
    
    /**
     * 创建常量整数
     * 
     * @param value 整数值
     * @param bits 位数（8、16、32 或 64）
     */
    public ConstantInt(long value, int bits) {
        super(getIntType(bits), "i" + bits + "_" + value);
        this.value = value;
    }
    
    /**
     * 创建 8 位常量整数
     * 
     * @param value 整数值
     * @return 常量整数
     */
    public static ConstantInt getI8(byte value) {
        return new ConstantInt(value, 8);
    }
    
    /**
     * 创建 16 位常量整数
     * 
     * @param value 整数值
     * @return 常量整数
     */
    public static ConstantInt getI16(short value) {
        return new ConstantInt(value, 16);
    }
    
    /**
     * 创建 32 位常量整数
     * 
     * @param value 整数值
     * @return 常量整数
     */
    public static ConstantInt getI32(int value) {
        return new ConstantInt(value, 32);
    }
    
    /**
     * 创建 64 位常量整数
     * 
     * @param value 整数值
     * @return 常量整数
     */
    public static ConstantInt getI64(long value) {
        return new ConstantInt(value, 64);
    }
    
    /**
     * 创建布尔常量
     * 
     * @param value 布尔值
     * @return 常量整数
     */
    public static ConstantInt getBool(boolean value) {
        return new ConstantInt(value ? 1 : 0, 1);
    }
    
    @Override
    public Long getValue() {
        return value;
    }
    
    /**
     * 获取整数类型
     * 
     * @param bits 位数
     * @return 整数类型
     */
    private static IRType getIntType(int bits) {
        IRTypeFactory factory = IRTypeFactory.getInstance();
        
        switch (bits) {
            case 1:
                return factory.getBoolType();
            case 8:
                return factory.getInt8Type();
            case 16:
                return factory.getInt16Type();
            case 32:
                return factory.getInt32Type();
            case 64:
                return factory.getInt64Type();
            default:
                throw new IllegalArgumentException("Invalid integer bit width: " + bits);
        }
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitConstantInt(this);
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}