package org.tabooproject.fluxon.optimization;

import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompilationPhase;
import org.tabooproject.fluxon.ir.IRModule;
import org.tabooproject.fluxon.optimization.passes.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 优化器
 * 协调各个优化 Pass 的执行
 */
public class Optimizer implements CompilationPhase<IRModule> {
    private final List<OptimizationPass> passes = new ArrayList<>();
    private final boolean debug;
    
    /**
     * 创建优化器
     * 
     * @param debug 是否启用调试模式
     */
    public Optimizer(boolean debug) {
        this.debug = debug;
        
        // 添加优化 Pass
        passes.add(new ConstantFolding());
        passes.add(new DeadCodeElimination());
        passes.add(new CommonSubexpressionElimination());
        passes.add(new SelectiveStaticization());
    }
    
    /**
     * 创建优化器
     */
    public Optimizer() {
        this(false);
    }
    
    /**
     * 添加优化 Pass
     * 
     * @param pass 优化 Pass
     */
    public void addPass(OptimizationPass pass) {
        passes.add(pass);
    }
    
    /**
     * 执行优化
     * 
     * @param context 编译上下文
     * @return 优化后的 IR 模块
     */
    @Override
    public IRModule process(CompilationContext context) {
        // 获取 IR 模块
        IRModule module = (IRModule) context.getAttribute("irModule");
        
        if (module == null) {
            throw new IllegalStateException("IR module not found in compilation context");
        }
        
        // 应用优化 Pass
        for (OptimizationPass pass : passes) {
            if (debug) {
                System.out.println("Applying optimization pass: " + pass.getName());
            }
            
            module = pass.optimize(module);
            
            if (debug) {
                System.out.println("After " + pass.getName() + ":");
                System.out.println(module);
            }
        }
        
        // 将优化后的 IR 模块添加到编译上下文
        context.setAttribute("optimizedIrModule", module);
        
        return module;
    }
}