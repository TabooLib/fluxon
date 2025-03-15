package org.tabooproject.fluxon.ir.type;

/**
 * 整数类型
 * 表示整数值的类型
 */
public class IntegerType extends AbstractIRType {
    private final int size;
    
    /**
     * 创建整数类型
     * 
     * @param name 类型名称
     * @param size 类型大小（字节）
     */
    public IntegerType(String name, int size) {
        super(name);
        this.size = size;
    }
    
    @Override
    public int getSize() {
        return size;
    }
    
    @Override
    public boolean isIntegerType() {
        return true;
    }
    
    @Override
    public boolean isBooleanType() {
        return "bool".equals(name);
    }
}