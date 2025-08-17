package org.tabooproject.fluxon.parser.definition;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.VariablePosition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        private final LinkedHashMap<String, VariablePosition> parameters;
        private final ParseResult body;
        private final boolean isAsync;
        private final List<Annotation> annotations;

        public FunctionDefinition(String name, LinkedHashMap<String, VariablePosition> parameters, ParseResult body, boolean isAsync, List<Annotation> annotations) {
            this.name = name;
            this.parameters = parameters;
            this.body = body;
            this.isAsync = isAsync;
            this.annotations = annotations;
        }

        public String getName() {
            return name;
        }

        public LinkedHashMap<String, VariablePosition> getParameters() {
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
            sb.append(String.join(", ", parameters.keySet()));
            sb.append(") = ");
            
            // 函数体
            sb.append(body.toPseudoCode());
            return sb.toString();
        }
    }
}