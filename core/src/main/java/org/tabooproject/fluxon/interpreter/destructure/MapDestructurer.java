package org.tabooproject.fluxon.interpreter.destructure;

import org.tabooproject.fluxon.runtime.Environment;

import java.util.Map;

/**
 * Map 解构器
 * 将 Map 解构为键值对
 */
public class MapDestructurer extends AbstractDestructurer {
    
    @Override
    public boolean supports(Object element) {
        return element instanceof Map;
    }
    
    @Override
    public void destructure(Environment environment, Map<String, Integer> variables, Object element) {
        if (variables.isEmpty()) {
            return;
        }
        Map<?, ?> map = (Map<?, ?>) element;
        for (Map.Entry<String, Integer> entry : variables.entrySet()) {
            environment.assign(entry.getKey(), map.get(entry.getKey()), entry.getValue());
        }
    }
} 