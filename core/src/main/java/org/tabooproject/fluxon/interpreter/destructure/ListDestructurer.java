package org.tabooproject.fluxon.interpreter.destructure;

import org.tabooproject.fluxon.runtime.Environment;

import java.util.List;
import java.util.Map;

/**
 * 列表解构器
 * 将列表按索引解构为多个变量
 */
public class ListDestructurer extends AbstractDestructurer {
    
    @Override
    public boolean supports(Object element) {
        return element instanceof List;
    }
    
    @Override
    public void destructure(Environment environment, Map<String, Integer> variables, Object element) {
        if (variables.isEmpty()) {
            return;
        }
        List<?> list = (List<?>) element;
        // 设置有值的变量
        int index = 0;
        for (Map.Entry<String, Integer> entry : variables.entrySet()) {
            if (index < list.size()) {
                environment.assign(entry.getKey(), list.get(index), entry.getValue());
            } else {
                break;
            }
            index++;
        }
        // 设置剩余变量为 null
        fillRemainingVariables(environment, variables, list.size());
    }
} 