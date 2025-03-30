package org.tabooproject.fluxon.interpreter.destructure;

import org.tabooproject.fluxon.runtime.Environment;

import java.util.List;

/**
 * 解构器接口
 * 定义解构元素到环境变量的行为
 */
public interface Destructurer {
    
    /**
     * 判断当前解构器是否支持处理给定元素
     * 
     * @param element 要解构的元素
     * @return 是否支持处理该元素
     */
    boolean supports(Object element);
    
    /**
     * 将元素解构到环境中
     * 
     * @param environment 目标环境
     * @param variables 变量名列表
     * @param element 要解构的元素
     */
    void destructure(Environment environment, List<String> variables, Object element);
} 