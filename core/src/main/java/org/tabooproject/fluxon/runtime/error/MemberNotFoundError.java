package org.tabooproject.fluxon.runtime.error;

import org.tabooproject.fluxon.runtime.FluxonRuntimeError;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 成员未找到错误
 * 当反射访问的成员（字段或方法）不存在时抛出
 */
public class MemberNotFoundError extends FluxonRuntimeError {

    public MemberNotFoundError(Class<?> clazz, String memberName) {
        super("Member '" + memberName + "' not found in class " + clazz.getName());
    }

    public MemberNotFoundError(Class<?> clazz, String methodName, Class<?>[] argTypes) {
        super("Method '" + methodName + "' with parameter types [" + formatTypes(argTypes) + "] not found in class " + clazz.getName());
    }

    private static String formatTypes(Class<?>[] types) {
        return Arrays.stream(types)
                .map(t -> t == null ? "null" : t.getSimpleName())
                .collect(Collectors.joining(", "));
    }
}
