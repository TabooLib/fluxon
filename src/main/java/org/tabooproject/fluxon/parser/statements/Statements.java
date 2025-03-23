package org.tabooproject.fluxon.parser.statements;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.util.StringUtils;

import java.util.List;

/**
 * 语句类集合
 * 包含各种语句的实现类
 */
public class Statements {

    /**
     * 表达式语句
     * 表示一个表达式作为语句使用
     */
    public static class ExpressionStatement implements Statement {
        private final ParseResult expression;

        public ExpressionStatement(ParseResult expression) {
            this.expression = expression;
        }

        public ParseResult getExpression() {
            return expression;
        }

        @Override
        public String toString() {
            return "ExpressionStatement(" + expression + ")";
        }

        @Override
        public String toPseudoCode() {
            return expression.toPseudoCode();
        }
    }

    /**
     * 代码块
     * 表示一组语句组成的代码块
     */
    public static class Block implements Statement {

        private final String label;
        private final List<ParseResult> statements;

        public Block(String label, List<ParseResult> statements) {
            this.label = label;
            this.statements = statements;
        }

        public String getLabel() {
            return label;
        }

        public List<ParseResult> getStatements() {
            return statements;
        }

        @Override
        public String toString() {
            return "Block(" + statements + ")";
        }

        @Override
        public String toPseudoCode() {
            StringBuilder sb = new StringBuilder();
            if (label != null) {
                sb.append("@").append(label).append(" ");
            }
            sb.append("{");
            for (ParseResult stmt : statements) {
                sb.append(stmt.toPseudoCode()).append(";");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * 返回语句
     * 表示函数的返回语句
     */
    public static class ReturnStatement implements Statement {
        private final ParseResult value;

        public ReturnStatement(ParseResult value) {
            this.value = value;
        }

        public ParseResult getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Return(" + (value != null ? value : "") + ")";
        }

        @Override
        public String toPseudoCode() {
            return "return" + (value != null ? " " + value.toPseudoCode() : "");
        }
    }
}