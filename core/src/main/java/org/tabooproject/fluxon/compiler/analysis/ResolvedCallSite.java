package org.tabooproject.fluxon.compiler.analysis;

import java.util.Arrays;
import java.util.Objects;

/**
 * 静态分析得到的函数调用点。
 * {@link #getResolvedArgs()} 与调用处实参个数一致；下标 i 为第 i 个实参的编译期静态值，
 * 未解析则为 {@code null}。标量、{@link org.tabooproject.fluxon.parser.expression.LambdaExpression} 等 AST 节点可直接作为槽位值。
 */
public final class ResolvedCallSite {

    private final String functionName;
    private final String enclosingFunction;
    private final Object[] resolvedArgs;

    public ResolvedCallSite(String functionName, String enclosingFunction, Object[] resolvedArgs) {
        this.functionName = functionName;
        this.enclosingFunction = enclosingFunction;
        this.resolvedArgs = resolvedArgs == null ? new Object[0] : resolvedArgs.clone();
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getEnclosingFunction() {
        return enclosingFunction;
    }

    public Object[] getResolvedArgs() {
        return resolvedArgs.clone();
    }

    public int getArgumentCount() {
        return resolvedArgs.length;
    }

    /**
     * 第 index 个实参的静态值；越界或未解析时返回 {@code null}。
     */
    public Object getResolvedArg(int index) {
        if (index < 0 || index >= resolvedArgs.length) {
            return null;
        }
        return resolvedArgs[index];
    }

    /**
     * 第 index 个实参在静态解析为字符串时的值，否则 {@code null}。
     */
    public String getResolvedStringArg(int index) {
        Object value = getResolvedArg(index);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResolvedCallSite)) {
            return false;
        }
        ResolvedCallSite that = (ResolvedCallSite) o;
        return Objects.equals(functionName, that.functionName)
                && Objects.equals(enclosingFunction, that.enclosingFunction)
                && Arrays.deepEquals(resolvedArgs, that.resolvedArgs);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(functionName, enclosingFunction);
        result = 31 * result + Arrays.deepHashCode(resolvedArgs);
        return result;
    }
}