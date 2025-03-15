package org.tabooproject.fluxon.semantic;

/**
 * 符号
 * 表示变量、函数等符号的信息
 */
public class Symbol {
    private final String name;
    private final SymbolKind kind;
    private final TypeInfo type;
    private final boolean isMutable; // 是否可变（var vs val）
    
    /**
     * 创建符号
     * 
     * @param name 符号名
     * @param kind 符号类型
     * @param type 类型信息
     * @param isMutable 是否可变
     */
    public Symbol(String name, SymbolKind kind, TypeInfo type, boolean isMutable) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.isMutable = isMutable;
    }
    
    /**
     * 获取符号名
     * 
     * @return 符号名
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取符号类型
     * 
     * @return 符号类型
     */
    public SymbolKind getKind() {
        return kind;
    }
    
    /**
     * 获取类型信息
     * 
     * @return 类型信息
     */
    public TypeInfo getType() {
        return type;
    }
    
    /**
     * 检查是否可变
     * 
     * @return 是否可变
     */
    public boolean isMutable() {
        return isMutable;
    }
    
    /**
     * 符号类型
     */
    public enum SymbolKind {
        /**
         * 变量
         */
        VARIABLE,
        
        /**
         * 函数
         */
        FUNCTION,
        
        /**
         * 函数参数
         */
        PARAMETER,
        
        /**
         * 类型
         */
        TYPE
    }
}