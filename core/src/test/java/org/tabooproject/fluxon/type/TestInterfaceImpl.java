package org.tabooproject.fluxon.type;

/**
 * 实现多个接口的测试类
 *
 * @author sky
 */
public class TestInterfaceImpl implements TestInterface, TestSecondInterface {

    public String implField = "impl-field";

    @Override
    public String interfaceMethod() {
        return "interface-method";
    }

    @Override
    public String interfaceMethodWithArg(String arg) {
        return "interface:" + arg;
    }

    @Override
    public int getInterfaceValue() {
        return 42;
    }

    @Override
    public String secondInterfaceMethod() {
        return "second-interface";
    }

    @Override
    public String secondInterfaceWithArg(String arg) {
        return "second:" + arg;
    }

    // 重写 default 方法
    @Override
    public String overriddenDefault() {
        return "overridden-default";
    }

    // 实现类自己的方法
    public String implOnlyMethod() {
        return "impl-only";
    }

    // 返回 self 用于链式调用
    public TestInterfaceImpl getSelf() {
        return this;
    }

    // concat 方法用于测试
    public String concat(String a, String b) {
        return a + b;
    }
}
