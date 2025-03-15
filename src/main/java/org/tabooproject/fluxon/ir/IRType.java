package org.tabooproject.fluxon.ir;

/**
 * IR 类型接口
 * 表示中间表示中的类型
 */
public interface IRType {
    
    /**
     * 获取类型名称
     * 
     * @return 类型名称
     */
    String getName();
    
    /**
     * 获取类型大小（字节）
     * 
     * @return 类型大小
     */
    int getSize();
    
    /**
     * 检查是否为整数类型
     * 
     * @return 是否为整数类型
     */
    boolean isIntegerType();
    
    /**
     * 检查是否为浮点类型
     * 
     * @return 是否为浮点类型
     */
    boolean isFloatType();
    
    /**
     * 检查是否为布尔类型
     * 
     * @return 是否为布尔类型
     */
    boolean isBooleanType();
    
    /**
     * 检查是否为字符串类型
     * 
     * @return 是否为字符串类型
     */
    boolean isStringType();
    
    /**
     * 检查是否为指针类型
     * 
     * @return 是否为指针类型
     */
    boolean isPointerType();
    
    /**
     * 检查是否为函数类型
     * 
     * @return 是否为函数类型
     */
    boolean isFunctionType();
    
    /**
     * 检查是否为数组类型
     * 
     * @return 是否为数组类型
     */
    boolean isArrayType();
    
    /**
     * 检查是否为结构体类型
     * 
     * @return 是否为结构体类型
     */
    boolean isStructType();
    
    /**
     * 检查是否为 void 类型
     * 
     * @return 是否为 void 类型
     */
    boolean isVoidType();
}