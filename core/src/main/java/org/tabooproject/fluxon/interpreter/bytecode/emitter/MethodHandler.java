package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;

/**
 * 方法处理器函数式接口
 * 用于在分发策略中处理单个方法的字节码生成
 */
@FunctionalInterface
public interface MethodHandler {

    /**
     * 处理单个方法的字节码生成
     *
     * @param mv     方法访问器
     * @param method 要处理的方法
     */
    void handle(MethodVisitor mv, Method method);
}
