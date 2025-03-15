package org.tabooproject.fluxon.optimization;

import org.tabooproject.fluxon.ir.IRModule;

/**
 * 优化 Pass 接口
 * 定义优化 Pass 的统一接口
 */
public interface OptimizationPass {
    
    /**
     * 优化 IR 模块
     * 
     * @param module IR 模块
     * @return 优化后的 IR 模块
     */
    IRModule optimize(IRModule module);
    
    /**
     * 获取优化 Pass 名称
     * 
     * @return 优化 Pass 名称
     */
    String getName();
}