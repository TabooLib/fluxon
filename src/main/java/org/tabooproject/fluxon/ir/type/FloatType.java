package org.tabooproject.fluxon.ir.type;

/**
 * 浮点类型
 * 表示浮点值的类型
 */
public class FloatType extends AbstractIRType {
    private final int size;
    
    /**
     * 创建浮点类型
     * 
     * @param name 类型名称
     * @param size 类型大小（字节）
     */
    public FloatType(String name, int size) {
        super(name);
        this.size = size;
    }
    
    @Override
    public int getSize() {
        return size;
    }
    
    @Override
    public boolean isFloatType() {
        return true;
    }
}