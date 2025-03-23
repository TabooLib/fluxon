package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.util.StringUtils;
import org.tabooproject.fluxon.parser.statements.Statements.Block;

import java.util.List;

/**
 * 表达式类集合
 * 包含各种表达式的实现类
 */
public class Expressions {
    
    /**
     * 字面量表达式基类
     */
    public static abstract class Literal implements Expression {
        // 字面量基类，具体实现由子类提供
    }
    
    /**
     * 字符串字面量
     */
    public static class StringLiteral extends Literal {
        private final String value;
        
        public StringLiteral(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return "StringLiteral(" + value + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return "\"" + value + "\"";
        }
    }
    
    /**
     * 整数字面量
     */
    public static class IntegerLiteral extends Literal {
        private final String value;
        
        public IntegerLiteral(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return "IntegerLiteral(" + value + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return value;
        }
    }
    
    /**
     * 浮点数字面量
     */
    public static class FloatLiteral extends Literal {
        private final String value;
        
        public FloatLiteral(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return "FloatLiteral(" + value + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return value + "f";
        }
    }
    
    /**
     * 布尔字面量
     */
    public static class BooleanLiteral extends Literal {
        private final boolean value;
        
        public BooleanLiteral(boolean value) {
            this.value = value;
        }
        
        public boolean getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return "BooleanLiteral(" + value + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return String.valueOf(value);
        }
    }
    
    /**
     * 变量引用
     */
    public static class Variable implements Expression {
        private final String name;
        
        public Variable(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public String toString() {
            return "Variable(" + name + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return name;
        }
    }
    
    /**
     * 分组表达式
     */
    public static class GroupingExpression implements Expression {
        private final ParseResult expression;
        
        public GroupingExpression(ParseResult expression) {
            this.expression = expression;
        }
        
        public ParseResult getExpression() {
            return expression;
        }
        
        @Override
        public String toString() {
            return "Grouping(" + expression + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return "(" + expression.toPseudoCode() + ")";
        }
    }
    
    /**
     * 一元表达式
     */
    public static class UnaryExpression implements Expression {
        private final Token operator;
        private final ParseResult right;
        
        public UnaryExpression(Token operator, ParseResult right) {
            this.operator = operator;
            this.right = right;
        }
        
        public Token getOperator() {
            return operator;
        }
        
        public ParseResult getRight() {
            return right;
        }
        
        @Override
        public String toString() {
            return "Unary(" + operator.getValue() + ", " + right + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return operator.getValue() + right.toPseudoCode();
        }
    }
    
    /**
     * 二元表达式
     */
    public static class BinaryExpression implements Expression {
        private final ParseResult left;
        private final Token operator;
        private final ParseResult right;
        
        public BinaryExpression(ParseResult left, Token operator, ParseResult right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        
        public ParseResult getLeft() {
            return left;
        }
        
        public Token getOperator() {
            return operator;
        }
        
        public ParseResult getRight() {
            return right;
        }
        
        @Override
        public String toString() {
            return "Binary(" + left + ", " + operator.getValue() + ", " + right + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return left.toPseudoCode() + " " + operator.getValue() + " " + right.toPseudoCode();
        }
    }
    
    /**
     * 逻辑表达式
     */
    public static class LogicalExpression implements Expression {
        private final ParseResult left;
        private final Token operator;
        private final ParseResult right;
        
        public LogicalExpression(ParseResult left, Token operator, ParseResult right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        
        public ParseResult getLeft() {
            return left;
        }
        
        public Token getOperator() {
            return operator;
        }
        
        public ParseResult getRight() {
            return right;
        }
        
        @Override
        public String toString() {
            return "Logical(" + left + ", " + operator.getValue() + ", " + right + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return left.toPseudoCode() + " " + operator.getValue() + " " + right.toPseudoCode();
        }
    }
    
    /**
     * 赋值表达式
     */
    public static class Assignment implements Expression {
        private final String name;
        private final Token operator;
        private final ParseResult value;
        
        public Assignment(String name, Token operator, ParseResult value) {
            this.name = name;
            this.operator = operator;
            this.value = value;
        }
        
        public String getName() {
            return name;
        }
        
        public Token getOperator() {
            return operator;
        }
        
        public ParseResult getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return "Assignment(" + name + ", " + operator.getValue() + ", " + value + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return name + " " + operator.getValue() + " " + value.toPseudoCode();
        }
    }
    
    /**
     * 函数调用
     */
    public static class FunctionCall implements Expression {
        private final ParseResult callee;
        private final List<ParseResult> arguments;
        
        public FunctionCall(ParseResult callee, List<ParseResult> arguments) {
            this.callee = callee;
            this.arguments = arguments;
        }
        
        public ParseResult getCallee() {
            return callee;
        }
        
        public List<ParseResult> getArguments() {
            return arguments;
        }
        
        @Override
        public String toString() {
            return "Call(" + callee + ", " + arguments + ")";
        }
        
        @Override
        public String toPseudoCode() {
            StringBuilder sb = new StringBuilder();
            sb.append(callee.toPseudoCode()).append("(");
            // 参数列表
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(arguments.get(i).toPseudoCode());
            }
            return sb.append(")").toString();
        }
    }
    
    /**
     * Await 表达式
     */
    public static class AwaitExpression implements Expression {
        private final ParseResult expression;
        
        public AwaitExpression(ParseResult expression) {
            this.expression = expression;
        }
        
        public ParseResult getExpression() {
            return expression;
        }
        
        @Override
        public String toString() {
            return "Await(" + expression + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return "await " + expression.toPseudoCode();
        }
    }
    
    /**
     * 引用表达式（&变量）
     */
    public static class ReferenceExpression implements Expression {
        private final ParseResult expression;
        
        public ReferenceExpression(ParseResult expression) {
            this.expression = expression;
        }
        
        public ParseResult getExpression() {
            return expression;
        }
        
        @Override
        public String toString() {
            return "Reference(" + expression + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return "&" + expression.toPseudoCode();
        }
    }
    
    /**
     * If 表达式
     */
    public static class IfExpression implements Expression {
        private final ParseResult condition;
        private final ParseResult thenBranch;
        private final ParseResult elseBranch;
        
        public IfExpression(ParseResult condition, ParseResult thenBranch, ParseResult elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }
        
        public ParseResult getCondition() {
            return condition;
        }
        
        public ParseResult getThenBranch() {
            return thenBranch;
        }
        
        public ParseResult getElseBranch() {
            return elseBranch;
        }
        
        @Override
        public String toString() {
            return "If(" + condition + ", " + thenBranch + (elseBranch != null ? ", " + elseBranch : "") + ")";
        }
        
        @Override
        public String toPseudoCode() {
            StringBuilder sb = new StringBuilder();
            sb.append("if ").append(condition.toPseudoCode()).append(" then ");
            
            // 根据 thenBranch 的类型决定是否输出大括号
            if (thenBranch instanceof Block) {
                // 如果是代码块，直接输出
                sb.append(thenBranch.toPseudoCode());
            } else {
                // 如果是表达式，添加大括号
                sb.append("{").append(thenBranch.toPseudoCode()).append("}");
            }
            
            if (elseBranch != null) {
                sb.append(" else ");
                // 根据 elseBranch 的类型决定是否输出大括号
                if (elseBranch instanceof Block) {
                    sb.append(elseBranch.toPseudoCode());
                } else {
                    sb.append("{").append(elseBranch.toPseudoCode()).append("}");
                }
            }
            return sb.toString();
        }
        
    }
    
    /**
     * When 表达式分支
     */
    public static class WhenBranch {
        private final ParseResult condition;
        private final ParseResult result;
        
        public WhenBranch(ParseResult condition, ParseResult result) {
            this.condition = condition;
            this.result = result;
        }
        
        public ParseResult getCondition() {
            return condition;
        }
        
        public ParseResult getResult() {
            return result;
        }
        
        @Override
        public String toString() {
            return (condition != null ? condition : "else") + " -> " + result;
        }

        public String toPseudoCode() {
            String condStr = condition != null ? condition.toPseudoCode() : "else";
            return condStr + " -> " + result.toPseudoCode();
        }
    }
    
    /**
     * When 表达式
     */
    public static class WhenExpression implements Expression {
        private final ParseResult subject;
        private final List<WhenBranch> branches;
        
        public WhenExpression(ParseResult subject, List<WhenBranch> branches) {
            this.subject = subject;
            this.branches = branches;
        }
        
        public ParseResult getSubject() {
            return subject;
        }
        
        public List<WhenBranch> getBranches() {
            return branches;
        }
        
        @Override
        public String toString() {
            return "When(" + (subject != null ? subject : "") + ", " + branches + ")";
        }
        
        @Override
        public String toPseudoCode() {
            StringBuilder sb = new StringBuilder();

            sb.append("when ");
            
            if (subject != null) {
                sb.append(subject.toPseudoCode());
            }
            
            sb.append("{");
            for (WhenBranch branch : branches) {
                sb.append(branch.toPseudoCode()).append(";");
            }
            sb.append("}");
            return sb.toString();
        }
    }
    
    /**
     * 列表字面量
     */
    public static class ListLiteral implements Expression {
        private final List<ParseResult> elements;
        
        public ListLiteral(List<ParseResult> elements) {
            this.elements = elements;
        }
        
        public List<ParseResult> getElements() {
            return elements;
        }
        
        @Override
        public String toString() {
            return "ListLiteral(" + elements + ")";
        }
        
        @Override
        public String toPseudoCode() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            
            for (int i = 0; i < elements.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(elements.get(i).toPseudoCode());
            }
            sb.append("]");
            return sb.toString();
        }
    }
    
    /**
     * 字典字面量
     */
    public static class MapLiteral implements Expression {
        private final List<MapEntry> entries;
        
        public MapLiteral(List<MapEntry> entries) {
            this.entries = entries;
        }
        
        public List<MapEntry> getEntries() {
            return entries;
        }
        
        @Override
        public String toString() {
            return "MapLiteral(" + entries + ")";
        }
        
        @Override
        public String toPseudoCode() {
            if (entries.isEmpty()) {
                return "[:]";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(entries.get(i).toPseudoCode());
            }
            
            sb.append("]");
            return sb.toString();
        }
    }
    
    /**
     * 字典条目
     */
    public static class MapEntry {
        private final ParseResult key;
        private final ParseResult value;
        
        public MapEntry(ParseResult key, ParseResult value) {
            this.key = key;
            this.value = value;
        }
        
        public ParseResult getKey() {
            return key;
        }
        
        public ParseResult getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return key + ": " + value;
        }
        
        public String toPseudoCode() {
            return key.toPseudoCode() + ": " + value.toPseudoCode();
        }
    }
    
    /**
     * 范围表达式
     */
    public static class RangeExpression implements Expression {
        private final ParseResult start;
        private final ParseResult end;
        private final boolean inclusive; // true表示包含上界，false表示不包含上界
        
        public RangeExpression(ParseResult start, ParseResult end, boolean inclusive) {
            this.start = start;
            this.end = end;
            this.inclusive = inclusive;
        }
        
        public ParseResult getStart() {
            return start;
        }
        
        public ParseResult getEnd() {
            return end;
        }
        
        public boolean isInclusive() {
            return inclusive;
        }
        
        @Override
        public String toString() {
            return "Range(" + start + ", " + end + ", " + inclusive + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return start.toPseudoCode() + (inclusive ? ".." : "..<") + end.toPseudoCode();
        }
    }
    
    /**
     * Elvis操作符表达式
     */
    public static class ElvisExpression implements Expression {
        private final ParseResult condition;
        private final ParseResult alternative;
        
        public ElvisExpression(ParseResult condition, ParseResult alternative) {
            this.condition = condition;
            this.alternative = alternative;
        }
        
        public ParseResult getCondition() {
            return condition;
        }
        
        public ParseResult getAlternative() {
            return alternative;
        }
        
        @Override
        public String toString() {
            return "Elvis(" + condition + ", " + alternative + ")";
        }
        
        @Override
        public String toPseudoCode() {
            return condition.toPseudoCode() + " ?: " + alternative.toPseudoCode();
        }
    }
    
    /**
     * While表达式
     */
    public static class WhileExpression implements Expression {
        private final ParseResult condition;
        private final ParseResult body;
        
        public WhileExpression(ParseResult condition, ParseResult body) {
            this.condition = condition;
            this.body = body;
        }
        
        public ParseResult getCondition() {
            return condition;
        }
        
        public ParseResult getBody() {
            return body;
        }
        
        @Override
        public String toString() {
            return "While(" + condition + ", " + body + ")";
        }
        
        @Override
        public String toPseudoCode() {
            StringBuilder sb = new StringBuilder();
            sb.append("while ").append(condition.toPseudoCode()).append(" ");
            // 输出循环体
            if (body instanceof Block) {
                sb.append(body.toPseudoCode());
            } else {
                sb.append("{").append(body.toPseudoCode()).append("}");
            }
            return sb.toString();
        }
    }
}