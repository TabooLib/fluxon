package org.tabooproject.fluxon.runtime.java;

import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;

/**
 * 方法处理器函数式接口
 */
@FunctionalInterface
public interface MethodHandler {

    void handle(MethodVisitor mv, Method method);
}