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
        public String toPseudoCode(int indent) {
            return StringUtils.getIndent(indent) + expression.toPseudoCode(0);
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
        public String toPseudoCode(int indent) {
            StringBuilder sb = new StringBuilder();
            String indentStr = StringUtils.getIndent(indent);
            sb.append(indentStr);
            if (label != null) {
                sb.append("@").append(label).append(" ");
            }
            sb.append("{\n");
            for (ParseResult stmt : statements) {
                sb.append(stmt.toPseudoCode(indent + 1)).append("\n");
            }
            sb.append(indentStr).append("}");
            return sb.toString();
        }
    }
    
    /**
     * 变量声明
     * 表示变量的声明和可选的初始化
     */
    public static class VariableDeclaration implements Statement {
        private final String name;
        private final boolean isVal; // true 表示不可变变量 (val)，false 表示可变变量 (var)
        private final ParseResult initializer;
        
        public VariableDeclaration(String name, boolean isVal, ParseResult initializer) {
            this.name = name;
            this.isVal = isVal;
            this.initializer = initializer;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isVal() {
            return isVal;
        }
        
        public ParseResult getInitializer() {
            return initializer;
        }
        
        @Override
        public String toString() {
            return "VariableDeclaration(" + (isVal ? "val" : "var") + ", " + 
                   name + ", " + initializer + ")";
        }
        
        @Override
        public String toPseudoCode(int indent) {
            String indentStr = StringUtils.getIndent(indent);
            return indentStr + (isVal ? "val " : "var ") + name + " = " + initializer.toPseudoCode(0);
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
        public String toPseudoCode(int indent) {
            String indentStr = StringUtils.getIndent(indent);
            return indentStr + "return" + (value != null ? " " + value.toPseudoCode(0) : "");
        }
    }
}