package org.tabooproject.fluxon.optimization.passes;

import org.tabooproject.fluxon.ir.IRBasicBlock;
import org.tabooproject.fluxon.ir.IRFunction;
import org.tabooproject.fluxon.ir.IRInstruction;
import org.tabooproject.fluxon.ir.IRModule;
import org.tabooproject.fluxon.ir.instruction.CallInst;

import java.util.*;

/**
 * 选择性静态化优化 Pass
 * 将符合条件的函数静态化，以提高性能
 */
public class SelectiveStaticization extends AbstractOptimizationPass {
    
    /**
     * 创建选择性静态化优化 Pass
     */
    public SelectiveStaticization() {
        super("SelectiveStaticization");
    }
    
    @Override
    public IRModule optimize(IRModule module) {
        // 构建调用图
        CallGraph callGraph = buildCallGraph(module);
        
        // 识别可以静态化的函数
        Set<IRFunction> staticFunctions = identifyStaticFunctions(callGraph);
        
        // 静态化函数
        for (IRFunction function : staticFunctions) {
            staticizeFunction(function);
        }
        
        return module;
    }
    
    /**
     * 构建调用图
     * 
     * @param module IR 模块
     * @return 调用图
     */
    private CallGraph buildCallGraph(IRModule module) {
        CallGraph callGraph = new CallGraph();
        
        // 添加所有函数
        for (IRFunction function : module.getFunctions()) {
            callGraph.addFunction(function);
        }
        
        // 添加调用关系
        for (IRFunction caller : module.getFunctions()) {
            for (IRBasicBlock block : caller.getBlocks()) {
                for (IRInstruction instruction : block.getInstructions()) {
                    if (instruction instanceof CallInst) {
                        CallInst call = (CallInst) instruction;
                        
                        // 获取被调用的函数
                        if (call.getFunction() instanceof IRFunction) {
                            IRFunction callee = (IRFunction) call.getFunction();
                            callGraph.addCall(caller, callee);
                        }
                    }
                }
            }
        }
        
        return callGraph;
    }
    
    /**
     * 识别可以静态化的函数
     * 
     * @param callGraph 调用图
     * @return 可以静态化的函数集合
     */
    private Set<IRFunction> identifyStaticFunctions(CallGraph callGraph) {
        Set<IRFunction> staticFunctions = new HashSet<>();
        
        // 获取所有函数
        Set<IRFunction> allFunctions = callGraph.getAllFunctions();
        
        // 对每个函数进行分析
        for (IRFunction function : allFunctions) {
            // 如果函数满足静态化条件，添加到集合
            if (canBeStaticized(function, callGraph)) {
                staticFunctions.add(function);
            }
        }
        
        return staticFunctions;
    }
    
    /**
     * 检查函数是否可以静态化
     * 
     * @param function 函数
     * @param callGraph 调用图
     * @return 是否可以静态化
     */
    private boolean canBeStaticized(IRFunction function, CallGraph callGraph) {
        // 检查函数是否被多次调用
        if (callGraph.getCallers(function).size() <= 1) {
            return false;
        }
        
        // 检查函数是否足够小
        if (isFunctionTooLarge(function)) {
            return false;
        }
        
        // 检查函数是否有副作用
        if (hasSideEffects(function)) {
            return false;
        }
        
        // 检查函数是否递归
        if (isRecursive(function, callGraph)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查函数是否过大
     * 
     * @param function 函数
     * @return 是否过大
     */
    private boolean isFunctionTooLarge(IRFunction function) {
        // 统计指令数量
        int instructionCount = 0;
        
        for (IRBasicBlock block : function.getBlocks()) {
            instructionCount += block.getInstructions().size();
        }
        
        // 如果指令数量超过阈值，认为函数过大
        return instructionCount > 50;
    }
    
    /**
     * 检查函数是否有副作用
     * 
     * @param function 函数
     * @return 是否有副作用
     */
    private boolean hasSideEffects(IRFunction function) {
        // 检查函数中的指令是否有副作用
        for (IRBasicBlock block : function.getBlocks()) {
            for (IRInstruction instruction : block.getInstructions()) {
                if (instruction.hasSideEffects()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查函数是否递归
     * 
     * @param function 函数
     * @param callGraph 调用图
     * @return 是否递归
     */
    private boolean isRecursive(IRFunction function, CallGraph callGraph) {
        // 检查函数是否直接递归
        if (callGraph.getCalls(function).contains(function)) {
            return true;
        }
        
        // 检查函数是否间接递归
        Set<IRFunction> visited = new HashSet<>();
        return isIndirectlyRecursive(function, function, callGraph, visited);
    }
    
    /**
     * 检查函数是否间接递归
     * 
     * @param root 根函数
     * @param current 当前函数
     * @param callGraph 调用图
     * @param visited 已访问函数集合
     * @return 是否间接递归
     */
    private boolean isIndirectlyRecursive(IRFunction root, IRFunction current, CallGraph callGraph, Set<IRFunction> visited) {
        // 标记当前函数为已访问
        visited.add(current);
        
        // 检查当前函数的调用
        for (IRFunction callee : callGraph.getCalls(current)) {
            // 如果调用了根函数，则是递归的
            if (callee == root) {
                return true;
            }
            
            // 如果调用了未访问的函数，递归检查
            if (!visited.contains(callee)) {
                if (isIndirectlyRecursive(root, callee, callGraph, visited)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 静态化函数
     * 
     * @param function 函数
     */
    private void staticizeFunction(IRFunction function) {
        // 标记函数为静态
        // 在实际实现中，需要修改函数的属性或名称，以表示它是静态的
        // 这里简化处理，只是打印日志
        System.out.println("Staticizing function: " + function.getName());
    }
    
    /**
     * 调用图
     * 表示函数之间的调用关系
     */
    private static class CallGraph {
        // 函数到其调用者的映射
        private final Map<IRFunction, Set<IRFunction>> callers = new HashMap<>();
        
        // 函数到其被调用的函数的映射
        private final Map<IRFunction, Set<IRFunction>> calls = new HashMap<>();
        
        /**
         * 添加函数
         * 
         * @param function 函数
         */
        public void addFunction(IRFunction function) {
            callers.putIfAbsent(function, new HashSet<>());
            calls.putIfAbsent(function, new HashSet<>());
        }
        
        /**
         * 添加调用关系
         * 
         * @param caller 调用者
         * @param callee 被调用者
         */
        public void addCall(IRFunction caller, IRFunction callee) {
            // 添加函数
            addFunction(caller);
            addFunction(callee);
            
            // 添加调用关系
            callers.get(callee).add(caller);
            calls.get(caller).add(callee);
        }
        
        /**
         * 获取函数的调用者
         * 
         * @param function 函数
         * @return 调用者集合
         */
        public Set<IRFunction> getCallers(IRFunction function) {
            return callers.getOrDefault(function, Collections.emptySet());
        }
        
        /**
         * 获取函数调用的函数
         * 
         * @param function 函数
         * @return 被调用的函数集合
         */
        public Set<IRFunction> getCalls(IRFunction function) {
            return calls.getOrDefault(function, Collections.emptySet());
        }
        
        /**
         * 获取所有函数
         * 
         * @return 所有函数集合
         */
        public Set<IRFunction> getAllFunctions() {
            return callers.keySet();
        }
    }
}