package org.tabooproject.fluxon.parser.definition;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.Collections;
import java.util.Map;

/**
 * 注解定义
 * 表示函数或其他元素上的注解
 * 
 * @author sky
 */
public class Annotation implements ParseResult {
    private final String name;
    private final Map<String, Object> attributes;

    public Annotation(String name) {
        this(name, Collections.emptyMap());
    }

    public Annotation(String name, Map<String, Object> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        if (attributes.isEmpty()) {
            return "@" + name;
        }
        return "@" + name + "(" + attributes + ")";
    }

    @Override
    public ResultType getType() {
        return ResultType.ANNOTATION;
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder("@").append(name);
        if (!attributes.isEmpty()) {
            sb.append("(");
            boolean first = true;
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append(" = ").append(entry.getValue());
                first = false;
            }
            sb.append(")");
        }
        return sb.toString();
    }
}