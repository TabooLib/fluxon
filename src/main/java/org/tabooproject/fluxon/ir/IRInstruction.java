package org.tabooproject.fluxon.ir;

/**
 * IR 指令接口
 * 表示中间表示中的指令
 */
public interface IRInstruction extends IRValue {
    
    /**
     * 获取指令所在的基本块
     * 
     * @return 基本块
     */
    IRBasicBlock getParent();
    
    /**
     * 设置指令所在的基本块
     * 
     * @param parent 基本块
     */
    void setParent(IRBasicBlock parent);
    
    /**
     * 检查指令是否为终止指令
     * 终止指令是指会导致控制流转移的指令，如返回、分支等
     * 
     * @return 是否为终止指令
     */
    boolean isTerminator();
    
    /**
     * 检查指令是否有副作用
     * 有副作用的指令是指会改变程序状态的指令，如存储、函数调用等
     * 
     * @return 是否有副作用
     */
    boolean hasSideEffects();
    
    @Override
    default boolean isInstruction() {
        return true;
    }
}