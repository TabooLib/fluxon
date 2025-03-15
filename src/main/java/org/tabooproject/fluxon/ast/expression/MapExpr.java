package org.tabooproject.fluxon.ast.expression;

import org.tabooproject.fluxon.ast.AstVisitor;
import org.tabooproject.fluxon.ast.SourceLocation;

import java.util.List;

/**
 * 字典表达式节点
 * 表示字典字面量，如 { name: "Bob", age: 25 }
 */
public class MapExpr extends Expr {
    private final List<Entry> entries;
    
    /**
     * 创建字典表达式节点
     * 
     * @param entries 键值对列表
     * @param location 源代码位置
     */
    public MapExpr(List<Entry> entries, SourceLocation location) {
        super(location);
        this.entries = entries;
    }
    
    /**
     * 获取键值对列表
     * 
     * @return 键值对列表
     */
    public List<Entry> getEntries() {
        return entries;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitMapExpr(this);
    }
    
    /**
     * 字典键值对
     */
    public static class Entry {
        private final Expr key;
        private final Expr value;
        
        /**
         * 创建字典键值对
         * 
         * @param key 键
         * @param value 值
         */
        public Entry(Expr key, Expr value) {
            this.key = key;
            this.value = value;
        }
        
        /**
         * 获取键
         * 
         * @return 键
         */
        public Expr getKey() {
            return key;
        }
        
        /**
         * 获取值
         * 
         * @return 值
         */
        public Expr getValue() {
            return value;
        }
    }
}