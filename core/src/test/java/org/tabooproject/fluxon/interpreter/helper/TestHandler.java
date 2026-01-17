package org.tabooproject.fluxon.interpreter.helper;

/**
 * 测试用的抽象类 - 多方法覆写
 *
 * @author sky
 */
public abstract class TestHandler {

    public abstract String onStart();

    public abstract String onStop();

    public abstract String onData(String data);
}
