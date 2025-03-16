package org.tabooproject.fluxon.parser;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类型定义
 * 表示 Fluxon 中的类型，包含属性和函数
 */
public class Type {

    private final String name;
    private final Map<String, Property> properties = new HashMap<>();
    private final Map<String, List<Function>> functions = new HashMap<>();

    // 原始类型（Java Class对象）
    private Class<?> runtimeClass;

    /**
     * 创建带有运行时类的类型
     * 
     * @param name 类型名称
     * @param runtimeClass 运行时类
     */
    public Type(String name, Class<?> runtimeClass) {
        this.name = name;
        this.runtimeClass = runtimeClass;
    }
    /**
     * 获取类型名称
     * 
     * @return 类型名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取运行时类
     * 
     * @return 运行时类，如果未设置则返回 null
     */
    public Class<?> getRuntimeClass() {
        return runtimeClass;
    }
    
    /**
     * 添加属性
     * 
     * @param property 属性
     */
    public void addProperty(Property property) {
        properties.put(property.getName(), property);
    }
    
    /**
     * 添加属性（带反射字段）
     * 
     * @param name 属性名称
     * @param type 属性类型
     * @param field 反射字段
     */
    public void addProperty(String name, String type, Field field) {
        Property property = new Property(name, type);
        property.setField(field);
        properties.put(name, property);
    }
    
    /**
     * 添加函数
     * 
     * @param function 函数
     */
    public void addFunction(Function function) {
        // 支持函数重载，同名函数存储在列表中
        functions.computeIfAbsent(function.getName(), k -> new ArrayList<>()).add(function);
    }
    
    /**
     * 添加函数（带反射方法）
     * 
     * @param name 函数名称
     * @param parameters 参数列表
     * @param returnType 返回类型
     * @param method 反射方法
     */
    public void addFunction(String name, List<Parameter> parameters, String returnType, Method method) {
        Function function = new Function(name, parameters, returnType);
        function.setMethod(method);
        functions.computeIfAbsent(name, k -> new ArrayList<>()).add(function);
    }
    
    /**
     * 检查是否包含指定名称的属性
     * 
     * @param name 属性名称
     * @return 是否包含属性
     */
    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }
    
    /**
     * 获取属性
     * 
     * @param name 属性名称
     * @return 属性，如果不存在则返回 null
     */
    public Property getProperty(String name) {
        return properties.get(name);
    }
    
    /**
     * 检查是否包含指定名称的函数
     * 
     * @param name 函数名称
     * @return 是否包含函数
     */
    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }
    
    /**
     * 获取函数列表
     * 
     * @param name 函数名称
     * @return 函数列表，如果不存在则返回 null
     */
    public List<Function> getFunctions(String name) {
        return functions.get(name);
    }
    
    /**
     * 根据参数类型匹配函数
     * 
     * @param name 函数名称
     * @param argTypes 参数类型列表
     * @return 匹配的函数，如果没有匹配则返回 null
     */
    public Function matchFunction(String name, List<String> argTypes) {
        if (!functions.containsKey(name)) {
            return null;
        }
        
        List<Function> candidates = functions.get(name);
        
        // 精确匹配
        for (Function function : candidates) {
            if (function.matchParameters(argTypes)) {
                return function;
            }
        }
        
        // 如果没有精确匹配，尝试找到参数数量相同的函数
        for (Function function : candidates) {
            if (function.getParameters().size() == argTypes.size()) {
                return function;
            }
        }
        
        // 如果还没有匹配，返回第一个函数
        return candidates.isEmpty() ? null : candidates.get(0);
    }
    
    /**
     * 设置运行时类
     * 
     * @param runtimeClass 运行时类
     */
    public void setRuntimeClass(Class<?> runtimeClass) {
        this.runtimeClass = runtimeClass;
    }
    
    @Override
    public String toString() {
        return "Type{" +
                "name='" + name + '\'' +
                ", properties=" + properties +
                ", functions=" + functions +
                ", runtimeClass=" + runtimeClass +
                '}';
    }
    
    /**
     * 属性定义
     */
    public static class Property {
        private final String name;
        private final String type;
        private Field field; // 反射字段
        
        /**
         * 创建属性
         * 
         * @param name 属性名称
         * @param type 属性类型
         */
        public Property(String name, String type) {
            this.name = name;
            this.type = type;
        }
        
        /**
         * 获取属性名称
         * 
         * @return 属性名称
         */
        public String getName() {
            return name;
        }
        
        /**
         * 获取属性类型
         * 
         * @return 属性类型
         */
        public String getType() {
            return type;
        }
        
        /**
         * 获取反射字段
         * 
         * @return 反射字段，如果未设置则返回 null
         */
        public Field getField() {
            return field;
        }
        
        /**
         * 设置反射字段
         * 
         * @param field 反射字段
         */
        public void setField(Field field) {
            this.field = field;
        }

        @Override
        public String toString() {
            return "Property{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", field=" + field +
                    '}';
        }
    }
    
    /**
     * 函数定义
     */
    public static class Function {
        private final String name;
        private final List<Parameter> parameters;
        private final String returnType;
        private Method method; // 反射方法
        
        /**
         * 创建函数
         * 
         * @param name 函数名称
         * @param parameters 参数列表
         * @param returnType 返回类型
         */
        public Function(String name, List<Parameter> parameters, String returnType) {
            this.name = name;
            this.parameters = parameters;
            this.returnType = returnType;
        }
        
        /**
         * 获取函数名称
         * 
         * @return 函数名称
         */
        public String getName() {
            return name;
        }
        
        /**
         * 获取参数列表
         * 
         * @return 参数列表
         */
        public List<Parameter> getParameters() {
            return parameters;
        }
        
        /**
         * 获取返回类型
         * 
         * @return 返回类型
         */
        public String getReturnType() {
            return returnType;
        }
        
        /**
         * 获取反射方法
         * 
         * @return 反射方法，如果未设置则返回 null
         */
        public Method getMethod() {
            return method;
        }
        
        /**
         * 设置反射方法
         * 
         * @param method 反射方法
         */
        public void setMethod(Method method) {
            this.method = method;
        }
        
        /**
         * 检查参数类型是否匹配
         * 
         * @param argTypes 参数类型列表
         * @return 是否匹配
         */
        public boolean matchParameters(List<String> argTypes) {
            if (parameters.size() != argTypes.size()) {
                return false;
            }
            
            for (int i = 0; i < parameters.size(); i++) {
                if (!parameters.get(i).getType().equals(argTypes.get(i))) {
                    return false;
                }
            }
            
            return true;
        }
        
        /**
         * 从反射方法创建函数
         * 
         * @param method 反射方法
         * @return 函数
         */
        public static Function fromMethod(Method method) {
            String name = method.getName();
            java.lang.reflect.Parameter[] methodParams = method.getParameters();
            List<Parameter> parameters = new ArrayList<>();
            
            for (java.lang.reflect.Parameter param : methodParams) {
                String paramName = param.getName();
                String paramType = param.getType().getSimpleName();
                parameters.add(new Parameter(paramName, paramType));
            }
            
            return new Function(name, parameters, method.getReturnType().getSimpleName());
        }

        @Override
        public String toString() {
            return "Function{" +
                    "name='" + name + '\'' +
                    ", parameters=" + parameters +
                    ", returnType='" + returnType + '\'' +
                    ", method=" + method +
                    '}';
        }
    }
    
    /**
     * 参数定义
     */
    public static class Parameter {
        private final String name;
        private final String type;
        private Class<?> runtimeType; // 运行时类型
        
        /**
         * 创建参数
         * 
         * @param name 参数名称
         * @param type 参数类型
         */
        public Parameter(String name, String type) {
            this.name = name;
            this.type = type;
        }
        
        /**
         * 创建带运行时类型的参数
         * 
         * @param name 参数名称
         * @param type 参数类型
         * @param runtimeType 运行时类型
         */
        public Parameter(String name, String type, Class<?> runtimeType) {
            this(name, type);
            this.runtimeType = runtimeType;
        }
        /**
         * 获取参数名称
         * 
         * @return 参数名称
         */
        public String getName() {
            return name;
        }
        
        /**
         * 获取参数类型
         * 
         * @return 参数类型
         */
        public String getType() {
            return type;
        }
        
        /**
         * 获取运行时类型
         * 
         * @return 运行时类型，如果未设置则返回 null
         */
        public Class<?> getRuntimeType() {
            return runtimeType;
        }
        
        /**
         * 设置运行时类型
         * 
         * @param runtimeType 运行时类型
         */
        public void setRuntimeType(Class<?> runtimeType) {
            this.runtimeType = runtimeType;
        }

        @Override
        public String toString() {
            return "Parameter{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", runtimeType=" + runtimeType +
                    '}';
        }
    }
}