package org.tabooproject.fluxon.interpreter.destructure;

import org.tabooproject.fluxon.runtime.Environment;

import java.util.List;
import java.util.Map;

/**
 * Map.Entry 解构器
 * 将 Map.Entry 解构为键值对
 */
public class MapEntryDestructurer extends AbstractDestructurer {
    
    @Override
    public boolean supports(Object element) {
        return element instanceof Map.Entry;
    }
    
    @Override
    public void destructure(Environment environment, List<String> variables, Object element) {
        if (variables.isEmpty()) {
            return;
        }
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) element;
        // 第一个变量为键
        environment.defineRootVariable(variables.get(0), entry.getKey());
        // 如果有第二个变量，则为值
        if (variables.size() >= 2) {
            environment.defineRootVariable(variables.get(1), entry.getValue());
        }
        // 设置剩余变量为 null
        fillRemainingVariables(environment, variables, 2);
    }
} 