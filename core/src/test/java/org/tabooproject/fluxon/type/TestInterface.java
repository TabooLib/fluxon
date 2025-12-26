package org.tabooproject.fluxon.type;

/**
 * 接口测试用的基础接口
 *
 * @author sky
 */
public interface TestInterface {

    String interfaceMethod();

    String interfaceMethodWithArg(String arg);

    default String defaultMethod() {
        return "default-impl";
    }

    default String defaultMethodWithArg(String arg) {
        return "default:" + arg;
    }
}
