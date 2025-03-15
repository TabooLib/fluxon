package org.tabooproject.fluxon.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IR 模块
 * 表示一个编译单元，包含全局变量和函数
 */
public class IRModule implements IRNode {
    private final String name;
    private final List<IRFunction> functions = new ArrayList<>();
    private final Map<String, IRValue> globals = new HashMap<>();
    
    /**
     * 创建 IR 模块
     * 
     * @param name 模块名称
     */
    public IRModule(String name) {
        this.name = name;
    }
    
    /**
     * 获取模块名称
     * 
     * @return 模块名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取函数列表
     * 
     * @return 函数列表
     */
    public List<IRFunction> getFunctions() {
        return functions;
    }
    
    /**
     * 添加函数
     * 
     * @param function 函数
     */
    public void addFunction(IRFunction function) {
        functions.add(function);
    }
    
    /**
     * 获取全局变量
     * 
     * @return 全局变量
     */
    public Map<String, IRValue> getGlobals() {
        return globals;
    }
    
    /**
     * 添加全局变量
     * 
     * @param name 变量名
     * @param value 变量值
     */
    public void addGlobal(String name, IRValue value) {
        globals.put(name, value);
    }
    
    /**
     * 获取全局变量
     * 
     * @param name 变量名
     * @return 变量值
     */
    public IRValue getGlobal(String name) {
        return globals.get(name);
    }
    
    /**
     * 获取函数
     * 
     * @param name 函数名
     * @return 函数
     */
    public IRFunction getFunction(String name) {
        for (IRFunction function : functions) {
            if (function.getName().equals(name)) {
                return function;
            }
        }
        return null;
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitModule(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("module ").append(name).append(" {\n");
        
        // 全局变量
        for (Map.Entry<String, IRValue> entry : globals.entrySet()) {
            sb.append("  global ").append(entry.getKey())
                    .append(" = ").append(entry.getValue())
                    .append("\n");
        }
        
        // 函数
        for (IRFunction function : functions) {
            sb.append(function).append("\n");
        }
        
        sb.append("}");
        return sb.toString();
    }
}