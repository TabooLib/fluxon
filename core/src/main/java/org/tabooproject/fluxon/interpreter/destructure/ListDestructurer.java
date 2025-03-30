package org.tabooproject.fluxon.interpreter.destructure;

import org.tabooproject.fluxon.runtime.Environment;
import java.util.List;

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
    public void destructure(Environment environment, List<String> variables, Object element) {
        if (variables.isEmpty()) {
            return;
        }
        List<?> list = (List<?>) element;
        // 设置有值的变量
        for (int i = 0; i < Math.min(variables.size(), list.size()); i++) {
            environment.defineVariable(variables.get(i), list.get(i));
        }
        // 设置剩余变量为 null
        fillRemainingVariables(environment, variables, list.size());
    }
} 