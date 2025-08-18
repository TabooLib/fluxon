package org.tabooproject.fluxon.runtime.java;

import org.tabooproject.fluxon.runtime.Type;

/**
 * 类桥接器抽象类
 * 为一个类的所有导出方法提供统一的调用入口
 */
public abstract class ClassBridge {

    public static final Type TYPE = new Type(ClassBridge.class);

    private final String[] supportedMethods;

    protected ClassBridge(String[] supportedMethods) {
        this.supportedMethods = supportedMethods;
    }

    /**
     * 调用指定的方法
     * 子类必须实现此方法来处理具体的方法调用
     *
     * @param methodName 方法名
     * @param instance   实例对象
     * @param args       方法参数
     * @return 方法返回值
     * @throws Exception 调用异常
     */
    public abstract Object invoke(String methodName, Object instance, Object... args);

    /**
     * 获取支持的方法列表
     *
     * @return 支持的方法名数组
     */
    public final String[] getSupportedMethods() {
        return supportedMethods.clone();
    }

    /**
     * 检查是否支持指定的方法
     *
     * @param methodName 方法名
     * @return 是否支持该方法
     */
    public final boolean supportsMethod(String methodName) {
        for (String method : supportedMethods) {
            if (method.equals(methodName)) {
                return true;
            }
        }
        return false;
    }
}
