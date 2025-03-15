package org.tabooproject.fluxon.ir.type;

import org.tabooproject.fluxon.ir.IRType;

/**
 * 指针类型
 * 表示指向其他类型的指针
 */
public class PointerType extends AbstractIRType {
    private final IRType elementType;
    
    /**
     * 创建指针类型
     * 
     * @param elementType 元素类型
     */
    public PointerType(IRType elementType) {
        super(elementType.getName() + "*");
        this.elementType = elementType;
    }
    
    /**
     * 获取元素类型
     * 
     * @return 元素类型
     */
    public IRType getElementType() {
        return elementType;
    }
    
    @Override
    public int getSize() {
        return 8; // 指针大小为 8 字节（64 位）
    }
    
    @Override
    public boolean isPointerType() {
        return true;
    }
    
    @Override
    public boolean isStringType() {
        // 字符指针被视为字符串
        return elementType.isIntegerType() && elementType.getSize() == 1;
    }
}