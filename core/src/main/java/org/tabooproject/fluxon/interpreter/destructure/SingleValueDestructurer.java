package org.tabooproject.fluxon.interpreter.destructure;

import org.tabooproject.fluxon.runtime.Environment;

import java.util.Map;

/**
 * 单值解构器
 * 将单个值解构为变量列表
 */
public class SingleValueDestructurer extends AbstractDestructurer {

    @Override
    public boolean supports(Object element) {
        // 这是默认的解构器，支持所有类型
        return true;
    }

    @Override
    public void destructure(Environment environment, Map<String, Integer> variables, Object element) {
        if (variables.isEmpty()) {
            return;
        }
        Map.Entry<String, Integer> entry = variables.entrySet().iterator().next();
        // 第一个变量为该值
        environment.assign(entry.getKey(), element, entry.getValue());
        // 设置剩余变量为 null
        fillRemainingVariables(environment, variables, 1);
    }
} 