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
        Class<?> varargArrayType = paramTypes[fixedParamCount];
        // 构建新的参数数组
        Object[] newArgs = new Object[paramTypes.length];
        // 复制固定参数
        System.arraycopy(args, 0, newArgs, 0, fixedParamCount);
        // 检查是否传入了单个匹配的数组参数（如 varargs(getArray()) 的情况）
        int varargCount = args.length - fixedParamCount;
        if (varargCount == 1) {
            Object singleArg = args[fixedParamCount];
            if (varargArrayType.isInstance(singleArg)) {
                // 传入的已经是匹配的数组类型，直接使用
                newArgs[fixedParamCount] = singleArg;
                return newArgs;
            }
        }
        // 打包 varargs 参数
        Class<?> varargType = varargArrayType.getComponentType();
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
}
