package org.tabooproject.fluxon.ir.type;

/**
 * Void 类型
 * 表示没有值的类型
 */
public class VoidType extends AbstractIRType {
    
    /**
     * 创建 Void 类型
     */
    public VoidType() {
        super("void");
    }
    
    @Override
    public int getSize() {
        return 0;
    }
    
    @Override
    public boolean isVoidType() {
        return true;
    }
}