package org.tabooproject.fluxon.runtime.reflection.resolve;

import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;
import org.tabooproject.fluxon.util.StringUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 字段解析器
 * 负责查找字段和 getter 方法
 */
public final class FieldResolver {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    private FieldResolver() {}

    /**
     * 查找字段（支持继承链和接口常量）
     */
    public static Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * 查找 getter 方法（getField, field, isField）
     */
    public static MethodHandle findGetterMethod(Class<?> clazz, String fieldName) throws IllegalAccessException {
        String capitalized = StringUtils.capitalize(fieldName);
        String[] patterns = {"get" + capitalized, fieldName, "is" + capitalized};
        for (String methodName : patterns) {
            try {
                Method method = clazz.getMethod(methodName);
                if (method.getParameterCount() == 0) {
                    return LOOKUP.unreflect(method);
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    /**
     * 尝试创建直接字段访问的 MethodHandle
     */
    public static MethodHandle tryCreateSpecializedFieldHandle(Class<?> targetClass, String fieldName) {
        try {
            Field field = targetClass.getField(fieldName);
            MethodHandle mh = LOOKUP.unreflectGetter(field);
            // 处理静态字段（无接收者参数）
            if (Modifier.isStatic(field.getModifiers())) {
                // 静态字段：添加一个被忽略的参数，使签名变为 (Object)Object
                mh = MethodHandles.dropArguments(mh, 0, Object.class);
            }
            // 适配为 (Object)Object 签名
            return mh.asType(MethodType.methodType(Object.class, Object.class));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 尝试创建 getter 方法的 MethodHandle
     */
    public static MethodHandle tryCreateGetterHandle(Class<?> targetClass, String fieldName) {
        String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String[] patterns = {"get" + capitalized, fieldName, "is" + capitalized};
        for (String methodName : patterns) {
            try {
                Method method = targetClass.getMethod(methodName);
                if (method.getParameterCount() == 0) {
                    MethodHandle mh = LOOKUP.unreflect(method);
                    // 适配为 (Object)Object 签名
                    return mh.asType(MethodType.methodType(Object.class, Object.class));
                }
            } catch (NoSuchMethodException | IllegalAccessException ignored) {
            }
        }
        return null;
    }

    /**
     * 获取 Lookup 实例
     */
    public static MethodHandles.Lookup getLookup() {
        return LOOKUP;
    }
}
