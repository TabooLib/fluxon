package org.tabooproject.fluxon.parser.definition;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.Collections;
import java.util.List;

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
        private final List<String> parameters;
        private final ParseResult body;
        private final boolean isAsync;
        private final List<Annotation> annotations;

        public FunctionDefinition(String name, List<String> parameters, ParseResult body, boolean isAsync) {
            this(name, parameters, body, isAsync, Collections.emptyList());
        }

        public FunctionDefinition(String name, List<String> parameters, ParseResult body, boolean isAsync, List<Annotation> annotations) {
            this.name = name;
            this.parameters = parameters;
            this.body = body;
            this.isAsync = isAsync;
            this.annotations = annotations;
        }

        public String getName() {
            return name;
        }

        public List<String> getParameters() {
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

        @Override
        public String toString() {
            return (isAsync ? "Async" : "") + "FunctionDefinition(" + name + ", " +
                    parameters + ", " + body + ", annotations=" + annotations + ")";
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
            sb.append(String.join(", ", parameters));
            sb.append(") = ");
            
            // 函数体
            sb.append(body.toPseudoCode());
            return sb.toString();
        }
    }
}