package org.tabooproject.fluxon.interpreter.destructure;

import org.tabooproject.fluxon.runtime.Environment;

import java.util.Map;

/**
 * 抽象解构器基类
 * 提供一些共用功能
 */
public abstract class AbstractDestructurer implements Destructurer {
    
    /**
     * 用 null 填充剩余的变量
     *
     * @param environment 目标环境
     * @param variables 变量名列表
     * @param fromIndex 开始填充的索引
     */
    protected void fillRemainingVariables(Environment environment, Map<String, Integer> variables, int fromIndex) {
        int index = 0;
        for (Map.Entry<String, Integer> entry : variables.entrySet()) {
            if (index >= fromIndex) {
                environment.assign(entry.getKey(), null, entry.getValue());
            }
            index++;
        }
    }
} 