package org.tabooproject.fluxon.compiler;

/**
 * 编译阶段接口
 * 定义编译过程中的各个阶段
 * 
 * @param <T> 阶段处理结果类型
 */
public interface CompilationPhase<T> {
    /**
     * 执行编译阶段
     * 
     * @param context 编译上下文
     * @return 阶段处理结果
     */
    T process(CompilationContext context);
}