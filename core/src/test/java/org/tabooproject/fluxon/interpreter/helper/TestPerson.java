package org.tabooproject.fluxon.interpreter.helper;

/**
 * 测试用的简单类 - 单个 String 参数
 */
public class TestPerson {

    private final String name;
    private final int age;

    public TestPerson(String name) {
        this.name = name;
        this.age = 0;
    }

    public TestPerson(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String greet() {
        return "Hello, " + name;
    }
}
