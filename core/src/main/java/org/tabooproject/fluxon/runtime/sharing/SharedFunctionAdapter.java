package org.tabooproject.fluxon.runtime.sharing;

import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.FunctionSignature;
import org.tabooproject.fluxon.runtime.NativeFunction;
import org.tabooproject.fluxon.runtime.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 * 将远端 MethodHandle 适配为本地 NativeFunction。
 *
 * 版本安全策略：
 * - entry 版本高于 CURRENT_VERSION 时返回 stub（调用时抛异常，不在 import 时 crash）
 * - 低版本 entry 只读已知字段，忽略末尾多余元素
 *
 * @author sky
 */
public final class SharedFunctionAdapter {

    /**
     * 将 entry 适配为本地 NativeFunction
     * 自动区分普通函数和扩展函数
     */
    public static NativeFunction<?> adapt(Object[] entry) {
        int entryVersion = SharedFunctionEntry.version(entry);
        String name = SharedFunctionEntry.name(entry);
        String owner = SharedFunctionEntry.owner(entry);
        // 版本不兼容：返回 stub
        if (entryVersion > SharedFunctionEntry.CURRENT_VERSION) {
            return new NativeFunction<>(null, name, FunctionSignature.returnsObject().noParams(), context -> {
                throw new UnsupportedOperationException(
                        "Shared function '" + owner + ":" + name + "' uses protocol v" + entryVersion
                        + " but this runtime only supports up to v" + SharedFunctionEntry.CURRENT_VERSION
                        + ". Update Fluxon to use this function.");
            });
        }
        MethodHandle handle = SharedFunctionEntry.handle(entry);
        boolean isExtension = SharedFunctionEntry.isExtension(entry);
        if (isExtension) {
            return adaptExtension(name, owner, handle);
        }
        return adaptFunction(name, owner, handle);
    }

    /**
     * 适配普通函数
     */
    private static NativeFunction<?> adaptFunction(String name, String owner, MethodHandle handle) {
        MethodType type = handle.type();
        FunctionSignature signature = buildSignature(type, 0);
        return new NativeFunction<>(null, name, signature, context -> {
            int argCount = context.getArgumentCount();
            Object[] args = new Object[argCount];
            for (int i = 0; i < argCount; i++) {
                args[i] = context.getArgBoxed(i);
            }
            invokeAndSetReturn(handle, args, context);
        });
    }

    /**
     * 适配扩展函数
     * MethodHandle 的第一个参数是 target 对象，需要从 context.getTarget() 获取
     */
    private static NativeFunction<?> adaptExtension(String name, String owner, MethodHandle handle) {
        MethodType type = handle.type();
        // 跳过第一个参数（target），只对剩余参数构建签名
        FunctionSignature signature = buildSignature(type, 1);
        return new NativeFunction<>(null, name, signature, context -> {
            Object target = context.getTarget();
            int argCount = context.getArgumentCount();
            Object[] args = new Object[argCount + 1];
            args[0] = target;
            for (int i = 0; i < argCount; i++) {
                args[i + 1] = context.getArgBoxed(i);
            }
            invokeAndSetReturn(handle, args, context);
        });
    }

    /**
     * 调用 MethodHandle 并设置返回值
     */
    private static void invokeAndSetReturn(MethodHandle handle, Object[] args, FunctionContext<?> context) {
        try {
            Object result = handle.invokeWithArguments(args);
            if (result == null) {
                context.setReturnRef(null);
            } else if (result instanceof Integer) {
                context.setReturnInt((Integer) result);
            } else if (result instanceof Long) {
                context.setReturnLong((Long) result);
            } else if (result instanceof Double) {
                context.setReturnDouble((Double) result);
            } else if (result instanceof Float) {
                context.setReturnFloat((Float) result);
            } else if (result instanceof Boolean) {
                context.setReturnBool((Boolean) result);
            } else {
                context.setReturnRef(result);
            }
        } catch (Throwable e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            if (e instanceof Error) throw (Error) e;
            throw new RuntimeException("Shared function invocation failed", e);
        }
    }

    /**
     * 从 MethodType 构建 FunctionSignature
     *
     * @param skipParams 跳过前 N 个参数（Extension Function 跳过 target）
     */
    static FunctionSignature buildSignature(MethodType methodType, int skipParams) {
        Type returnType = Type.fromClass(methodType.returnType());
        int totalParams = methodType.parameterCount();
        int sigParams = totalParams - skipParams;
        Type[] paramTypes = new Type[sigParams];
        for (int i = 0; i < sigParams; i++) {
            paramTypes[i] = Type.fromClass(methodType.parameterType(i + skipParams));
        }
        return FunctionSignature.returns(returnType).params(paramTypes);
    }
}
