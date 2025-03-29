package org.tabooproject.fluxon.interpreter;

import java.util.HashMap;
import java.util.Map;

/**
 * 环境类
 * 用于管理运行时的变量和函数
 */
public class Environment {
    
    // 变量存储
    private final Map<String, Object> values = new HashMap<>();
    
    // 父环境，用于实现作用域链
    private final Environment parent;
    
    /**
     * 创建顶层环境（全局环境）
     */
    public Environment() {
        this.parent = null;
    }
    
    /**
     * 创建子环境
     *
     * @param parent 父环境
     */
    public Environment(Environment parent) {
        this.parent = parent;
    }
    
    /**
     * 获取父环境
     *
     * @return 父环境
     */
    public Environment getParent() {
        return parent;
    }
    
    /**
     * 判断是否为全局环境
     *
     * @return 是否为全局环境
     */
    public boolean isGlobal() {
        return parent == null;
    }
    
    /**
     * 在当前环境中定义变量
     *
     * @param name 变量名
     * @param value 变量值
     */
    public void define(String name, Object value) {
        values.put(name, value);
    }
    
    /**
     * 获取变量值（递归查找所有父环境）
     *
     * @param name 变量名
     * @return 变量值
     * @throws RuntimeException 如果变量不存在
     */
    public Object get(String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        }
        
        if (parent != null) {
            return parent.get(name);
        }
        
        throw new RuntimeException("未定义的变量: " + name);
    }
    
    /**
     * 更新变量值（递归查找所有父环境）
     *
     * @param name 变量名
     * @param value 新的变量值
     * @throws RuntimeException 如果变量不存在
     */
    public void assign(String name, Object value) {
        if (values.containsKey(name)) {
            values.put(name, value);
            return;
        }
        
        if (parent != null) {
            parent.assign(name, value);
            return;
        }
        
        throw new RuntimeException("未定义的变量: " + name);
    }
    
    /**
     * 查找变量所在的环境
     *
     * @param name 变量名
     * @return 变量所在的环境，如果不存在则返回null
     */
    public Environment getEnvironment(String name) {
        if (values.containsKey(name)) {
            return this;
        }
        
        if (parent != null) {
            return parent.getEnvironment(name);
        }
        
        return null;
    }
} 