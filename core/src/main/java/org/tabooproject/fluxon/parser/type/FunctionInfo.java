package org.tabooproject.fluxon.parser.type;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.Callable;
import org.tabooproject.fluxon.parser.ExtensionFunctionPosition;
import org.tabooproject.fluxon.parser.FunctionPosition;
import org.tabooproject.fluxon.parser.Parser;

/**
 * 函数信息持有者
 * 用于封装函数查找的结果，包括普通函数和扩展函数
 */
public class FunctionInfo {

    private final Callable function;
    private final FunctionPosition position;
    private final ExtensionFunctionPosition extensionPosition;

    public FunctionInfo(@Nullable Callable function, FunctionPosition position, ExtensionFunctionPosition extensionPosition) {
        this.function = function;
        this.position = position;
        this.extensionPosition = extensionPosition;
    }

    /**
     * 从解析器中查找函数信息
     *
     * @param parser 解析器
     * @param name   函数名
     * @return 函数信息
     */
    public static FunctionInfo lookup(Parser parser, String name) {
        // 获取普通函数
        Callable function = parser.getFunction(name);
        FunctionPosition pos = function instanceof FunctionPosition ? (FunctionPosition) function : null;
        // 只有在上下文环境中才获取扩展函数
        ExtensionFunctionPosition exPos = null;
        if (parser.getSymbolEnvironment().isContextCall()) {
            exPos = parser.getExtensionFunction(name);
        }
        return new FunctionInfo(function, pos, exPos);
    }

    /**
     * 检查是否找到了函数（普通函数或扩展函数）
     *
     * @return 是否找到函数
     */
    public boolean isFound() {
        return position != null || extensionPosition != null || function != null;
    }

    /**
     * 获取函数调用可调用对象
     *
     * @return 函数调用可调用对象
     */
    @Nullable
    public Callable getFunction() {
        return function;
    }

    /**
     * 获取普通函数位置信息
     *
     * @return 函数位置，可能为 null
     */
    public FunctionPosition getPosition() {
        return position;
    }

    /**
     * 获取扩展函数位置信息
     *
     * @return 扩展函数位置，可能为 null
     */
    public ExtensionFunctionPosition getExtensionPosition() {
        return extensionPosition;
    }
}

