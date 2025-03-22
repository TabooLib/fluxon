package org.tabooproject.fluxon.parser.expressions;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;

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
            return "If(" + condition + ", " + thenBranch + 
                   (elseBranch != null ? ", " + elseBranch : "") + ")";
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
    }
}