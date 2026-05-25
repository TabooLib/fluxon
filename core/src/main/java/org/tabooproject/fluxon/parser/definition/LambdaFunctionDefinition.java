package org.tabooproject.fluxon.parser.definition;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.ParseResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * 代表由 lambda 生成的函数定义
 */
public class LambdaFunctionDefinition extends FunctionDefinition {

    private final String ownerClassName;
    private final int captureOffset;

    public LambdaFunctionDefinition(
            String name,
            @NotNull LinkedHashMap<String, Integer> parameters,
            @NotNull ParseResult body,
            boolean isAsync,
            boolean isPrimarySync,
            @NotNull List<Annotation> annotations,
            @NotNull Set<String> localVariables,
            String ownerClassName,
            int captureOffset
    ) {
        super(name, parameters, body, isAsync, isPrimarySync, annotations, localVariables, false);
        this.ownerClassName = ownerClassName;
        this.captureOffset = captureOffset;
    }

    public String getOwnerClassName() {
        return ownerClassName;
    }

    public int getCaptureOffset() {
        return captureOffset;
    }

    /**
     * 非捕获 Lambda 可以复用普通函数的 env-free 局部变量路径。
     * 真捕获 Lambda 仍依赖定义时 Environment 链读取父槽位。
     */
    @Override
    public boolean canUseEnvFreeLocals() {
        return captureOffset == 0 && super.canUseEnvFreeLocals();
    }
}
