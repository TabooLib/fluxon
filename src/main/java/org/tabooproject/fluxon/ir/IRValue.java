package org.tabooproject.fluxon.ir;

/**
 * IR 值接口
 * 表示中间表示中的值
 */
public interface IRValue extends IRNode {
    
    /**
     * 获取值的类型
     * 
     * @return 值的类型
     */
    IRType getType();
    
    /**
     * 获取值的名称
     * 
     * @return 值的名称
     */
    String getName();
    
    /**
     * 检查是否为常量
     * 
     * @return 是否为常量
     */
    boolean isConstant();
    
    /**
     * 检查是否为指令
     * 
     * @return 是否为指令
     */
    boolean isInstruction();
    
    /**
     * 检查是否为全局变量
     * 
     * @return 是否为全局变量
     */
    boolean isGlobalVariable();
    
    /**
     * 检查是否为参数
     * 
     * @return 是否为参数
     */
    boolean isParameter();
}