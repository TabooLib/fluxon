package org.tabooproject.fluxon.interpreter.destructure;

import org.tabooproject.fluxon.runtime.Environment;

import java.util.Map;

/**
 * 解构器工厂
 * 提供创建类型安全的解构器的方法
 */
public class DestructurerFactory {
    
    /**
     * 创建类型安全的解构器
     * 
     * @param <T> 支持解构的元素类型
     * @param supportedType 支持的类型
     * @param destructureFunction 解构逻辑函数
     * @return 类型安全的解构器
     */
    public static <T> Destructurer forType(Class<T> supportedType, DestructureFunction<T> destructureFunction) {
        return new TypedDestructurer<>(supportedType, destructureFunction);
    }
    
    /**
     * 类型安全的解构函数接口
     * 
     * @param <T> 支持解构的元素类型
     */
    @FunctionalInterface
    public interface DestructureFunction<T> {
        /**
         * 执行解构
         * 
         * @param environment 目标环境
         * @param variables 变量名列表
         * @param element 要解构的元素
         */
        void destructure(Environment environment, Map<String, Integer> variables, T element);
    }
    
    /**
     * 类型安全的解构器实现
     * 
     * @param <T> 支持解构的元素类型
     */
    private static class TypedDestructurer<T> extends AbstractDestructurer {
        private final Class<T> supportedType;
        private final DestructureFunction<T> destructureFunction;
        
        public TypedDestructurer(Class<T> supportedType, DestructureFunction<T> destructureFunction) {
            this.supportedType = supportedType;
            this.destructureFunction = destructureFunction;
        }
        
        @Override
        public boolean supports(Object element) {
            return supportedType.isInstance(element);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public void destructure(Environment environment, Map<String, Integer> variables, Object element) {
            destructureFunction.destructure(environment, variables, (T) element);
        }
    }
    
    /**
     * 注册一个类型安全的解构器到全局注册表
     * 
     * @param <T> 支持解构的元素类型
     * @param supportedType 支持的类型
     * @param destructureFunction 解构逻辑函数
     */
    public static <T> void register(Class<T> supportedType, DestructureFunction<T> destructureFunction) {
        Destructurer destructurer = forType(supportedType, destructureFunction);
        DestructuringRegistry.getInstance().registerDestructurer(destructurer);
    }
} 