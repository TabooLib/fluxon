package org.tabooproject.fluxon.ir.value;

import org.tabooproject.fluxon.ir.IRType;
import org.tabooproject.fluxon.ir.IRVisitor;
import org.tabooproject.fluxon.ir.type.IRTypeFactory;

/**
 * 常量字符串
 * 表示字符串常量值
 */
public class ConstantString extends Constant {
    private final String value;
    
    /**
     * 创建常量字符串
     * 
     * @param value 字符串值
     */
    public ConstantString(String value) {
        super(getStringType(), "str_" + value.hashCode());
        this.value = value;
    }
    
    @Override
    public String getValue() {
        return value;
    }
    
    /**
     * 获取字符串类型
     * 
     * @return 字符串类型
     */
    private static IRType getStringType() {
        // 字符串类型是字符指针类型
        IRTypeFactory factory = IRTypeFactory.getInstance();
        return factory.getPointerType(factory.getInt8Type());
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitConstantString(this);
    }
    
    @Override
    public String toString() {
        return "\"" + escapeString(value) + "\"";
    }
    
    /**
     * 转义字符串
     * 
     * @param str 字符串
     * @return 转义后的字符串
     */
    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\"", "\\\"");
    }
}