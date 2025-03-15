package org.tabooproject.fluxon.ir;

import org.tabooproject.fluxon.ir.type.FunctionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IR 函数
 * 表示一个函数，包含参数和基本块
 */
public class IRFunction implements IRValue {
    private final String name;
    private final FunctionType type;
    private final List<IRBasicBlock> blocks = new ArrayList<>();
    private final List<IRValue> parameters = new ArrayList<>();
    private final Map<String, IRValue> locals = new HashMap<>();
    private IRModule parent;
    private boolean async; // 是否为异步函数
    
    /**
     * 创建 IR 函数
     * 
     * @param name 函数名
     * @param type 函数类型
     * @param parent 父模块
     */
    public IRFunction(String name, FunctionType type, IRModule parent) {
        this.name = name;
        this.type = type;
        this.parent = parent;
        this.async = false; // 默认为非异步函数
    }
    
    /**
     * 获取函数名
     * 
     * @return 函数名
     */
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * 获取函数类型
     * 
     * @return 函数类型
     */
    @Override
    public FunctionType getType() {
        return type;
    }
    
    /**
     * 获取父模块
     * 
     * @return 父模块
     */
    public IRModule getParent() {
        return parent;
    }
    
    /**
     * 设置父模块
     * 
     * @param parent 父模块
     */
    public void setParent(IRModule parent) {
        this.parent = parent;
    }
    
    /**
     * 检查是否为异步函数
     * 
     * @return 是否为异步函数
     */
    public boolean isAsync() {
        return async;
    }
    
    /**
     * 设置是否为异步函数
     * 
     * @param async 是否为异步函数
     */
    public void setAsync(boolean async) {
        this.async = async;
    }
    
    /**
     * 获取基本块列表
     * 
     * @return 基本块列表
     */
    public List<IRBasicBlock> getBlocks() {
        return blocks;
    }
    
    /**
     * 添加基本块
     * 
     * @param block 基本块
     */
    public void addBlock(IRBasicBlock block) {
        blocks.add(block);
        block.setParent(this);
    }
    
    /**
     * 获取参数列表
     * 
     * @return 参数列表
     */
    public List<IRValue> getParameters() {
        return parameters;
    }
    
    /**
     * 添加参数
     * 
     * @param parameter 参数
     */
    public void addParameter(IRValue parameter) {
        parameters.add(parameter);
    }
    
    /**
     * 获取局部变量
     * 
     * @return 局部变量
     */
    public Map<String, IRValue> getLocals() {
        return locals;
    }
    
    /**
     * 添加局部变量
     * 
     * @param name 变量名
     * @param value 变量值
     */
    public void addLocal(String name, IRValue value) {
        locals.put(name, value);
    }
    
    /**
     * 获取局部变量
     * 
     * @param name 变量名
     * @return 变量值
     */
    public IRValue getLocal(String name) {
        return locals.get(name);
    }
    
    /**
     * 获取入口基本块
     * 
     * @return 入口基本块
     */
    public IRBasicBlock getEntryBlock() {
        if (blocks.isEmpty()) {
            return null;
        }
        return blocks.get(0);
    }
    
    /**
     * 创建新的基本块
     * 
     * @param name 基本块名称
     * @return 基本块
     */
    public IRBasicBlock createBlock(String name) {
        IRBasicBlock block = new IRBasicBlock(name, this);
        addBlock(block);
        return block;
    }
    
    @Override
    public <T> T accept(IRVisitor<T> visitor) {
        return visitor.visitFunction(this);
    }
    
    @Override
    public boolean isConstant() {
        return false;
    }
    
    @Override
    public boolean isInstruction() {
        return false;
    }
    
    @Override
    public boolean isGlobalVariable() {
        return false;
    }
    
    @Override
    public boolean isParameter() {
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(async ? "async " : "").append("function ").append(name).append(" {\n");
        
        // 参数
        sb.append("  parameters: ");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parameters.get(i));
        }
        sb.append("\n");
        
        // 基本块
        for (IRBasicBlock block : blocks) {
            sb.append(block).append("\n");
        }
        
        sb.append("}");
        return sb.toString();
    }
}