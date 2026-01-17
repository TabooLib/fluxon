package org.tabooproject.fluxon.interpreter.helper;

/**
 * 测试用的抽象类 - 需要覆写 process 方法
 */
public abstract class TestProcessor {

    private final String prefix;

    public TestProcessor(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public abstract String process(String input);

    public String run(String input) {
        return prefix + ": " + process(input);
    }
}
