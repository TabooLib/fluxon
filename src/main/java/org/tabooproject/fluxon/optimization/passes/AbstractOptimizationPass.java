package org.tabooproject.fluxon.optimization.passes;

import org.tabooproject.fluxon.ir.IRBasicBlock;
import org.tabooproject.fluxon.ir.IRFunction;
import org.tabooproject.fluxon.ir.IRModule;
import org.tabooproject.fluxon.optimization.OptimizationPass;

/**
 * 抽象优化 Pass
 * 提供优化 Pass 的基本实现
 */
public abstract class AbstractOptimizationPass implements OptimizationPass {
    private final String name;
    
    /**
     * 创建抽象优化 Pass
     * 
     * @param name 优化 Pass 名称
     */
    protected AbstractOptimizationPass(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public IRModule optimize(IRModule module) {
        // 优化模块
        optimizeModule(module);
        
        // 优化函数
        for (IRFunction function : module.getFunctions()) {
            optimizeFunction(function);
            
            // 优化基本块
            for (IRBasicBlock block : function.getBlocks()) {
                optimizeBasicBlock(block);
            }
        }
        
        return module;
    }
    
    /**
     * 优化模块
     * 
     * @param module IR 模块
     */
    protected void optimizeModule(IRModule module) {
        // 默认实现为空，子类可以覆盖
    }
    
    /**
     * 优化函数
     * 
     * @param function IR 函数
     */
    protected void optimizeFunction(IRFunction function) {
        // 默认实现为空，子类可以覆盖
    }
    
    /**
     * 优化基本块
     * 
     * @param block IR 基本块
     */
    protected void optimizeBasicBlock(IRBasicBlock block) {
        // 默认实现为空，子类可以覆盖
    }
}