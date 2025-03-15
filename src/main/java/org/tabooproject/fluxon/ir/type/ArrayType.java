package org.tabooproject.fluxon.ir.type;

import org.tabooproject.fluxon.ir.IRType;

/**
 * 数组类型
 * 表示元素数组
 */
public class ArrayType extends AbstractIRType {
    private final IRType elementType;
    private final int length;
    
    /**
     * 创建数组类型
     * 
     * @param elementType 元素类型
     * @param length 数组长度
     */
    public ArrayType(IRType elementType, int length) {
        super(elementType.getName() + "[" + length + "]");
        this.elementType = elementType;
        this.length = length;
    }
    
    /**
     * 获取元素类型
     * 
     * @return 元素类型
     */
    public IRType getElementType() {
        return elementType;
    }
    
    /**
     * 获取数组长度
     * 
     * @return 数组长度
     */
    public int getLength() {
        return length;
    }
    
    @Override
    public int getSize() {
        return elementType.getSize() * length;
    }
    
    @Override
    public boolean isArrayType() {
        return true;
    }
}