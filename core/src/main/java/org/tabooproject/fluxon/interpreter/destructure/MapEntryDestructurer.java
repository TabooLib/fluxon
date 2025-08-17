package org.tabooproject.fluxon.interpreter.destructure;

import org.tabooproject.fluxon.parser.VariablePosition;
import org.tabooproject.fluxon.runtime.Environment;

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
    public void destructure(Environment environment, Map<String, VariablePosition> variables, Object element) {
        if (variables.isEmpty()) {
            return;
        }
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) element;
        // 第一个变量为键
        int i = 0;
        for (Map.Entry<String, VariablePosition> varEntry : variables.entrySet()) {
            int level = varEntry.getValue().getLevel();
            int index = varEntry.getValue().getIndex();
            if (i == 0) {
                environment.assign(varEntry.getKey(), entry.getKey(), level,  index);
            } else if (i == 1) {
                environment.assign(varEntry.getKey(), entry.getValue(), level,  index);
            } else {
                break;
            }
            i++;
        }
        // 设置剩余变量为 null
        fillRemainingVariables(environment, variables, 2);
    }
} 