package org.tabooproject.fluxon.type;

/**
 * 接口测试用的基础接口
 *
 * @author sky
 */
public interface TestInterface {

    // 接口常量
    String INTERFACE_CONSTANT = "INTERFACE_CONSTANT";

    String interfaceMethod();

    String interfaceMethodWithArg(String arg);

    int getInterfaceValue();

    default String defaultMethod() {
        return "default-method";
    }

    default String defaultMethodWithArg(String arg) {
        return "default:" + arg;
    }

    default String overriddenDefault() {
        return "default-not-overridden";
    }
}
