package org.tabooproject.fluxon.interpreter.destructure;

import org.tabooproject.fluxon.parser.VariablePosition;
import org.tabooproject.fluxon.runtime.Environment;

import java.util.Map;

/**
 * 数组解构器
 * 将数组按索引解构为多个变量
 */
public class ArrayDestructurer extends AbstractDestructurer {
    
    @Override
    public boolean supports(Object element) {
        return element instanceof Object[];
    }
    
    @Override
    public void destructure(Environment environment, Map<String, VariablePosition> variables, Object element) {
        if (variables.isEmpty()) {
            return;
        }
        Object[] array = (Object[]) element;
        // 设置有值的变量
        int index = 0;
        for (Map.Entry<String, VariablePosition> entry : variables.entrySet()) {
            if (index < array.length) {
                environment.assign(entry.getKey(), array[index], entry.getValue().getLevel(), entry.getValue().getIndex());
            } else {
                break;
            }
            index++;
        }
        // 设置剩余变量为 null
        fillRemainingVariables(environment, variables, array.length);
    }
} 