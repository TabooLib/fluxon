package org.tabooproject.fluxon.runtime.reflection.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Array.set;

/**
 * Varargs 处理工具类
 * 提供可变参数方法和构造函数的参数打包与兼容性检查
 */
public final class VarargsHandler {

    private VarargsHandler() {}

    /**
     * 打包 varargs 参数
     * 将原始参数数组转换为符合 varargs 方法/构造函数签名的参数数组
     *
     * @param paramTypes 方法/构造函数的参数类型（最后一个是数组类型）
     * @param args       原始参数
     * @return 打包后的参数数组
     */
    public static Object[] packVarargsArguments(Class<?>[] paramTypes, Object[] args) {
        int fixedParamCount = paramTypes.length - 1;
        Class<?> varargType = paramTypes[fixedParamCount].getComponentType();
        // 构建新的参数数组
        Object[] newArgs = new Object[paramTypes.length];
        // 复制固定参数
        System.arraycopy(args, 0, newArgs, 0, fixedParamCount);
        // 打包 varargs 参数
        int varargCount = args.length - fixedParamCount;
        Object varargArray = newInstance(varargType, varargCount);
        for (int i = 0; i < varargCount; i++) {
            set(varargArray, i, args[fixedParamCount + i]);
        }
        newArgs[fixedParamCount] = varargArray;
        return newArgs;
    }

    /**
     * 调用 varargs 方法（处理参数打包）
     */
    public static Object invokeVarargsMethod(Method method, Object target, Object[] args) throws Throwable {
        Object[] newArgs = packVarargsArguments(method.getParameterTypes(), args);
        return method.invoke(target, newArgs);
    }

    /**
     * 调用 varargs 构造函数（处理参数打包）
     */
    public static Object invokeVarargsConstructor(Constructor<?> ctor, Object[] args) throws Throwable {
        Object[] newArgs = packVarargsArguments(ctor.getParameterTypes(), args);
        return ctor.newInstance(newArgs);
    }

    /**
     * 检查 varargs 方法是否匹配
     */
    public static boolean isVarargsMethodAssignable(Method method, Class<?>[] argTypes) {
        return isVarargsParametersCompatible(method.getParameterTypes(), argTypes);
    }

    /**
     * 检查 varargs 构造函数是否匹配
     */
    public static boolean isVarargsConstructorAssignable(Constructor<?> ctor, Class<?>[] argTypes) {
        return isVarargsParametersCompatible(ctor.getParameterTypes(), argTypes);
    }

    /**
     * 检查 varargs 参数类型是否兼容
     *
     * @param paramTypes 方法/构造函数的参数类型（最后一个是数组类型）
     * @param argTypes   实际参数类型
     * @return 是否兼容
     */
    public static boolean isVarargsParametersCompatible(Class<?>[] paramTypes, Class<?>[] argTypes) {
        int fixedParamCount = paramTypes.length - 1;
        if (argTypes.length < fixedParamCount) {
            return false;
        }
        // 检查固定参数
        for (int i = 0; i < fixedParamCount; i++) {
            if (!TypeCompatibility.isTypeCompatible(paramTypes[i], argTypes[i])) {
                return false;
            }
        }
        // 检查 varargs 参数
        Class<?> varargType = paramTypes[fixedParamCount].getComponentType();
        for (int i = fixedParamCount; i < argTypes.length; i++) {
            if (!TypeCompatibility.isTypeCompatible(varargType, argTypes[i])) {
                return false;
            }
        }
        return true;
    }
}
