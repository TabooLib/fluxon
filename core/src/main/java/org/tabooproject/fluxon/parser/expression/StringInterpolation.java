package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.ParseResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 字符串插值表达式
 * 表示包含嵌入表达式的字符串，如 "Hello ${name}!"
 * <p>
 * parts 列表按顺序包含字符串片段和表达式：
 * - 字符串片段用 StringPart 包装
 * - 嵌入的表达式直接存储为 ParseResult
 */
public class StringInterpolation extends Expression {

    private final List<ParseResult> parts;

    public StringInterpolation(List<ParseResult> parts) {
        super(ExpressionType.STRING_INTERPOLATION);
        this.parts = parts;
    }

    public List<ParseResult> getParts() {
        return parts;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.STRING_INTERPOLATION;
    }

    @Override
    public String toPseudoCode() {
        StringBuilder sb = new StringBuilder("\"");
        for (ParseResult part : parts) {
            if (part instanceof StringPart) {
                sb.append(((StringPart) part).getValue());
            } else {
                sb.append("${").append(part.toPseudoCode()).append("}");
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "StringInterpolation(" + parts + ")";
    }

    /**
     * 构建器，用于方便地构造 StringInterpolation
     */
    public static class Builder {
        private final List<ParseResult> parts = new ArrayList<>();

        public void addStringPart(String value) {
            if (value != null && !value.isEmpty()) {
                parts.add(new StringPart(value));
            }
        }

        public void addExpression(ParseResult expr) {
            parts.add(expr);
        }

        public StringInterpolation build() {
            return new StringInterpolation(parts);
        }

        public boolean isEmpty() {
            return parts.isEmpty();
        }

        public int size() {
            return parts.size();
        }
    }

    /**
     * 字符串片段包装类
     */
    public static class StringPart implements ParseResult {
        private final String value;

        public StringPart(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public ResultType getType() {
            return ResultType.EXPRESSION;
        }

        @Override
        public String toPseudoCode() {
            return "\"" + value + "\"";
        }

        @Override
        public String toString() {
            return "StringPart(" + value + ")";
        }
    }
}
