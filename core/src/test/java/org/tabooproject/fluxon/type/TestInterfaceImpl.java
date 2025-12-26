package org.tabooproject.fluxon.type;

/**
 * 实现多个接口的测试类
 *
 * @author sky
 */
public class TestInterfaceImpl implements TestInterface, TestSecondInterface {

    public String implField = "impl-field-value";

    @Override
    public String interfaceMethod() {
        return "interface-method-impl";
    }

    @Override
    public String interfaceMethodWithArg(String arg) {
        return "interface:" + arg;
    }

    @Override
    public String secondInterfaceMethod() {
        return "second-interface-impl";
    }

    @Override
    public int getInterfaceValue() {
        return 42;
    }

    // 重写 default 方法
    @Override
    public String defaultMethod() {
        return "overridden-default";
    }

    // 实现类自己的方法
    public String implOnlyMethod() {
        return "impl-only";
    }
}
