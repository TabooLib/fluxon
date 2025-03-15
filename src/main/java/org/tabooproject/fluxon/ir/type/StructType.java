package org.tabooproject.fluxon.ir.type;

import org.tabooproject.fluxon.ir.IRType;

import java.util.Arrays;

/**
 * 结构体类型
 * 表示结构体的类型
 */
public class StructType extends AbstractIRType {
    private final IRType[] fieldTypes;
    private final int[] fieldOffsets;
    private final int size;
    
    /**
     * 创建结构体类型
     * 
     * @param name 结构体名称
     * @param fieldTypes 字段类型
     */
    public StructType(String name, IRType... fieldTypes) {
        super(name);
        this.fieldTypes = fieldTypes;
        
        // 计算字段偏移量和结构体大小
        this.fieldOffsets = new int[fieldTypes.length];
        int offset = 0;
        
        for (int i = 0; i < fieldTypes.length; i++) {
            // 对齐字段
            int alignment = getAlignment(fieldTypes[i]);
            offset = align(offset, alignment);
            
            // 设置字段偏移量
            fieldOffsets[i] = offset;
            
            // 增加偏移量
            offset += fieldTypes[i].getSize();
        }
        
        // 对齐结构体大小
        this.size = align(offset, getAlignment());
    }
    
    /**
     * 获取字段类型
     * 
     * @return 字段类型
     */
    public IRType[] getFieldTypes() {
        return fieldTypes;
    }
    
    /**
     * 获取字段偏移量
     * 
     * @return 字段偏移量
     */
    public int[] getFieldOffsets() {
        return fieldOffsets;
    }
    
    /**
     * 获取字段偏移量
     * 
     * @param index 字段索引
     * @return 字段偏移量
     */
    public int getFieldOffset(int index) {
        if (index < 0 || index >= fieldOffsets.length) {
            throw new IndexOutOfBoundsException("Field index out of bounds: " + index);
        }
        return fieldOffsets[index];
    }
    
    @Override
    public int getSize() {
        return size;
    }
    
    @Override
    public boolean isStructType() {
        return true;
    }
    
    /**
     * 获取结构体对齐要求
     * 
     * @return 对齐要求
     */
    public int getAlignment() {
        // 结构体的对齐要求是其最大字段的对齐要求
        int maxAlignment = 1;
        
        for (IRType fieldType : fieldTypes) {
            int alignment = getAlignment(fieldType);
            if (alignment > maxAlignment) {
                maxAlignment = alignment;
            }
        }
        
        return maxAlignment;
    }
    
    /**
     * 获取类型的对齐要求
     * 
     * @param type 类型
     * @return 对齐要求
     */
    private int getAlignment(IRType type) {
        // 对齐要求通常是类型大小，但最大为 8
        return Math.min(type.getSize(), 8);
    }
    
    /**
     * 对齐偏移量
     * 
     * @param offset 偏移量
     * @param alignment 对齐要求
     * @return 对齐后的偏移量
     */
    private int align(int offset, int alignment) {
        return (offset + alignment - 1) & ~(alignment - 1);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        StructType that = (StructType) o;
        
        if (!name.equals(that.name)) return false;
        return Arrays.equals(fieldTypes, that.fieldTypes);
    }
    
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + Arrays.hashCode(fieldTypes);
        return result;
    }
}