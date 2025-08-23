package org.tabooproject.fluxon.runtime.java;

import java.lang.reflect.Method;

/**
 * 封装导出方法及其注解信息
 */
public class ExportMethod {
    
    private final Method method;
    private final boolean async;
    private final boolean sync;
    private final String transformedName;
    
    /**
     * 构造函数
     * 
     * @param method 原始方法
     * @param transformedName 转换后的方法名
     */
    public ExportMethod(Method method, String transformedName) {
        this.method = method;
        Export export = method.getAnnotation(Export.class);
        this.async = export != null && export.async();
        this.sync = export != null && export.sync();
        this.transformedName = transformedName;
    }
    
    /**
     * 获取原始方法
     */
    public Method getMethod() {
        return method;
    }
    
    /**
     * 是否为异步函数
     */
    public boolean isAsync() {
        return async;
    }
    
    /**
     * 是否为同步函数
     */
    public boolean isSync() {
        return sync;
    }
    
    /**
     * 获取转换后的方法名
     */
    public String getTransformedName() {
        return transformedName;
    }
    
    /**
     * 获取原始方法名
     */
    public String getOriginalName() {
        return method.getName();
    }
}