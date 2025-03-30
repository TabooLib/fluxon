package org.tabooproject.fluxon.interpreter.destructure;

import org.tabooproject.fluxon.runtime.Environment;

import java.util.List;

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
    public void destructure(Environment environment, List<String> variables, Object element) {
        if (variables.isEmpty()) {
            return;
        }
        Object[] array = (Object[]) element;
        // 设置有值的变量
        for (int i = 0; i < Math.min(variables.size(), array.length); i++) {
            environment.defineVariable(variables.get(i), array[i]);
        }
        // 设置剩余变量为 null
        fillRemainingVariables(environment, variables, array.length);
    }
} 