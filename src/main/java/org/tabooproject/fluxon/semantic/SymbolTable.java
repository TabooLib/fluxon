package org.tabooproject.fluxon.semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * 符号表
 * 用于管理符号和作用域
 */
public class SymbolTable {
    private final Stack<Map<String, Symbol>> scopes = new Stack<>();
    
    /**
     * 创建符号表
     */
    public SymbolTable() {
        // 创建全局作用域
        enterScope();
    }
    
    /**
     * 进入新的作用域
     */
    public void enterScope() {
        scopes.push(new HashMap<>());
    }
    
    /**
     * 退出当前作用域
     */
    public void exitScope() {
        if (!scopes.isEmpty()) {
            scopes.pop();
        }
    }
    
    /**
     * 定义符号
     * 
     * @param name 符号名
     * @param symbol 符号信息
     * @return 如果符号已存在于当前作用域，则返回 false；否则返回 true
     */
    public boolean define(String name, Symbol symbol) {
        if (scopes.isEmpty()) {
            return false;
        }
        
        Map<String, Symbol> currentScope = scopes.peek();
        if (currentScope.containsKey(name)) {
            return false;
        }
        
        currentScope.put(name, symbol);
        return true;
    }
    
    /**
     * 查找符号
     * 
     * @param name 符号名
     * @return 符号信息，如果未找到则返回 null
     */
    public Symbol resolve(String name) {
        // 从当前作用域开始查找，逐级向上
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Symbol symbol = scopes.get(i).get(name);
            if (symbol != null) {
                return symbol;
            }
        }
        return null;
    }
    
    /**
     * 检查符号是否已在当前作用域中定义
     * 
     * @param name 符号名
     * @return 是否已定义
     */
    public boolean isDefined(String name) {
        if (scopes.isEmpty()) {
            return false;
        }
        
        return scopes.peek().containsKey(name);
    }
    
    /**
     * 获取当前作用域深度
     * 
     * @return 作用域深度
     */
    public int getScopeDepth() {
        return scopes.size();
    }
}