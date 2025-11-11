package org.tabooproject.fluxon.parser;

/**
 * 捕获变量信息
 * 用于记录 lambda 捕获的外层作用域变量
 */
public class CapturedVariable {

    private final String name;
    private final int sourceIndex;   // 在定义处环境中的槽位
    private final int lambdaIndex;   // 在 lambda 环境中的槽位

    public CapturedVariable(String name, int sourceIndex, int lambdaIndex) {
        this.name = name;
        this.sourceIndex = sourceIndex;
        this.lambdaIndex = lambdaIndex;
    }

    public String getName() {
        return name;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public int getLambdaIndex() {
        return lambdaIndex;
    }

    @Override
    public String toString() {
        return "CapturedVariable{" +
                "name='" + name + '\'' +
                ", sourceIndex=" + sourceIndex +
                ", lambdaIndex=" + lambdaIndex +
                '}';
    }
}
