package org.tabooproject.fluxon.parser.definition;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.SourceTrace;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 函数定义
 * 表示函数的定义
 */
public class FunctionDefinition implements Definition {
    private final String name;
    private final LinkedHashMap<String, Integer> parameters;
    private final Map<Integer, Class<?>> parameterTypes;
    private final ParseResult body;
    private final boolean isAsync;
    private final boolean isPrimarySync;
    private final List<Annotation> annotations;
    private final Set<String> localVariables;
    private final boolean registerToRoot;
    private boolean variablesCapturedByChildren = false;

    public FunctionDefinition(
            String name,
            @NotNull LinkedHashMap<String, Integer> parameters,
            @NotNull ParseResult body,
            boolean isAsync,
            boolean isPrimarySync,
            @NotNull List<Annotation> annotations,
            @NotNull Set<String> localVariables
    ) {
        this(name, parameters, Collections.emptyMap(), body, isAsync, isPrimarySync, annotations, localVariables, true);
    }

    public FunctionDefinition(
            String name,
            @NotNull LinkedHashMap<String, Integer> parameters,
            @NotNull ParseResult body,
            boolean isAsync,
            boolean isPrimarySync,
            @NotNull List<Annotation> annotations,
            @NotNull Set<String> localVariables,
            boolean registerToRoot
    ) {
        this(name, parameters, Collections.emptyMap(), body, isAsync, isPrimarySync, annotations, localVariables, registerToRoot);
    }

    public FunctionDefinition(
            String name,
            @NotNull LinkedHashMap<String, Integer> parameters,
            @NotNull Map<Integer, Class<?>> parameterTypes,
            @NotNull ParseResult body,
            boolean isAsync,
            boolean isPrimarySync,
            @NotNull List<Annotation> annotations,
            @NotNull Set<String> localVariables,
            boolean registerToRoot
    ) {
        this.name = name;
        this.parameters = parameters;
        this.parameterTypes = parameterTypes;
        this.body = body;
        this.isAsync = isAsync;
        this.isPrimarySync = isPrimarySync;
        this.annotations = annotations;
        this.localVariables = localVariables;
        this.registerToRoot = registerToRoot;
    }

    public String getName() {
        return name;
    }

    public LinkedHashMap<String, Integer> getParameters() {
        return parameters;
    }

    public Map<Integer, Class<?>> getParameterTypes() {
        return parameterTypes;
    }

    public ParseResult getBody() {
        return body;
    }

    public boolean isAsync() {
        return isAsync;
    }

    public boolean isPrimarySync() {
        return isPrimarySync;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public Set<String> getLocalVariables() {
        return localVariables;
    }

    public boolean isRegisterToRoot() {
        return registerToRoot;
    }

    /**
     * 是否有局部变量被子 Lambda 捕获
     * 用于 env-free 优化的资格判断
     */
    public boolean hasVariablesCapturedByChildren() {
        return variablesCapturedByChildren;
    }

    /**
     * 是否可以把局部变量放到 FunctionContext/JVM 槽位，跳过函数 Environment 局部数组。
     */
    public boolean canUseEnvFreeLocals() {
        return !variablesCapturedByChildren;
    }

    public void setVariablesCapturedByChildren(boolean v) {
        this.variablesCapturedByChildren = v;
    }

    @Override
    public String toString() {
        return "FunctionDefinition{" +
                "name='" + name + '\'' +
                ", parameters=" + parameters +
                ", body=" + body +
                ", isAsync=" + isAsync +
                ", isPrimarySync=" + isPrimarySync +
                ", annotations=" + annotations +
                ", localVariables=" + localVariables +
                ", registerToRoot=" + registerToRoot +
                '}';
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder();

        // 注解
        for (Annotation annotation : annotations) {
            sb.append(annotation.toPseudoCode()).append("\n");
        }

        // 函数声明
        if (isAsync) {
            sb.append("async ");
        }
        if (isPrimarySync) {
            sb.append("sync ");
        }
        sb.append("def ").append(name).append("(");

        // 参数列表
        sb.append(String.join(", ", parameters.keySet()));
        sb.append(") = ");

        // 函数体
        sb.append(body.toPseudoCode());
        return sb.toString();
    }
}
