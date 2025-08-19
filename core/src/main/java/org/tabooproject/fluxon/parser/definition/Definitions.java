package org.tabooproject.fluxon.parser.definition;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.ParseResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * 定义类集合
 * 包含各种定义的实现类
 */
public class Definitions {

    /**
     * 函数定义
     * 表示函数的定义
     */
    public static class FunctionDefinition implements Definition {
        private final String name;
        private final LinkedHashMap<String, Integer> parameters;
        private final ParseResult body;
        private final boolean isAsync;
        private final List<Annotation> annotations;
        private final Set<String> localVariables;

        public FunctionDefinition(
                String name,
                @NotNull LinkedHashMap<String, Integer> parameters,
                @NotNull ParseResult body,
                boolean isAsync,
                @NotNull List<Annotation> annotations,
                @NotNull Set<String> localVariables
        ) {
            this.name = name;
            this.parameters = parameters;
            this.body = body;
            this.isAsync = isAsync;
            this.annotations = annotations;
            this.localVariables = localVariables;
        }

        public String getName() {
            return name;
        }

        public LinkedHashMap<String, Integer> getParameters() {
            return parameters;
        }

        public ParseResult getBody() {
            return body;
        }

        public boolean isAsync() {
            return isAsync;
        }

        public List<Annotation> getAnnotations() {
            return annotations;
        }

        public Set<String> getLocalVariables() {
            return localVariables;
        }

        @Override
        public String toString() {
            return "FunctionDefinition{" +
                    "name='" + name + '\'' +
                    ", parameters=" + parameters +
                    ", body=" + body +
                    ", isAsync=" + isAsync +
                    ", annotations=" + annotations +
                    ", localVariables=" + localVariables +
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
            sb.append("def ").append(name).append("(");

            // 参数列表
            sb.append(String.join(", ", parameters.keySet()));
            sb.append(") = ");

            // 函数体
            sb.append(body.toPseudoCode());
            return sb.toString();
        }
    }
}