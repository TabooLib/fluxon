package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompilationPhase;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.definitions.Definitions.FunctionDefinition;
import org.tabooproject.fluxon.parser.expressions.Expressions.*;
import org.tabooproject.fluxon.parser.expressions.Expressions.MapEntry;
import org.tabooproject.fluxon.parser.impl.StatementParser;
import org.tabooproject.fluxon.parser.statements.Statements;
import org.tabooproject.fluxon.parser.statements.Statements.Block;
import org.tabooproject.fluxon.parser.statements.Statements.ExpressionStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fluxon解析器
 * <p>
 * 解析优先级（从高到低）：
 * 1. Primary - 基本表达式（字面量、变量、括号表达式、when表达式、if表达式、while表达式、列表字面量、字典字面量等）
 * 2. Call - 函数调用
 * 3. Unary - 一元表达式（!、-、await、&）
 * 4. Factor - 因子表达式（*、/、%）
 * 5. Term - 项表达式（+、-）
 * 6. Comparison - 比较表达式（>、>=、<、<=）
 * 7. Equality - 相等性表达式（==、!=）
 * 8. Range - 范围表达式（..、..<）
 * 9. LogicalAnd - 逻辑与表达式（&&）
 * 10. LogicalOr - 逻辑或表达式（||）
 * 11. Elvis - Elvis操作符（?:）
 * 12. Assignment - 赋值表达式（=、+=、-=、*=、/=）
 * <p>
 * 解析器使用递归下降解析方法，确保高优先级的操作符先于低优先级的操作符被处理。
 */
public class Parser implements CompilationPhase<List<ParseResult>> {

    // 符号表，用于跟踪已定义的函数和变量
    private final Map<String, SymbolInfo> symbolTable = new HashMap<>();

    private List<Token> tokens;
    private int position = 0;
    private Token currentToken;

    // 已经解析出的结果
    private List<ParseResult> results;

    /**
     * 创建解析器
     */
    public Parser() {
        initDefaultSymbols();
    }

    /**
     * 创建带有符号表的解析器
     */
    public Parser(Map<String, SymbolInfo> symbolTable) {
        initDefaultSymbols();
        this.symbolTable.putAll(symbolTable);
    }

    /**
     * 初始化系统符号表
     */
    public void initDefaultSymbols() {
        // 预先添加一些常用函数到符号表，用于测试
        symbolTable.put("print", new SymbolInfo(SymbolType.FUNCTION, "print", 1));
        symbolTable.put("checkGrade", new SymbolInfo(SymbolType.FUNCTION, "checkGrade", 1));
        symbolTable.put("player", new SymbolInfo(SymbolType.FUNCTION, "player", List.of(1, 3)));
        symbolTable.put("fetch", new SymbolInfo(SymbolType.FUNCTION, "fetch", 1));
    }

    /**
     * 执行解析
     *
     * @param context 编译上下文
     * @return 解析结果列表
     */
    @Override
    public List<ParseResult> process(CompilationContext context) {
        // 从上下文中获取词法单元序列
        List<Token> tokens = context.getAttribute("tokens");
        if (tokens == null) {
            throw new IllegalStateException("No tokens found in compilation context");
        }
        this.tokens = tokens;
        // 预加载第一个词法单元
        if (!tokens.isEmpty()) {
            currentToken = tokens.get(0);
        }
        this.position = 0;
        this.results = new ArrayList<>();
        // 解析顶层语句
        while (!isAtEnd()) {
            results.add(StatementParser.parse(this));
        }
        return results;
    }

    /**
     * 将解析结果转换为伪代码
     *
     * @return 伪代码字符串
     */
    public String toPseudoCode() {
        if (results == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ParseResult result : results) {
            sb.append(result.toPseudoCode(0));
            sb.append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 检查当前标记是否为表达式结束标记
     *
     * @return 是否为表达式结束标记
     */
    public boolean isEndOfExpression() {
        return check(TokenType.SEMICOLON) || check(TokenType.RIGHT_PAREN) ||
                check(TokenType.RIGHT_BRACE) || check(TokenType.RIGHT_BRACKET) ||
                check(TokenType.COMMA) || isAtEnd();
    }

    /**
     * 检查当前标记是否为操作符
     *
     * @return 是否为操作符
     */
    public boolean isOperator() {
        return check(TokenType.PLUS) || check(TokenType.MINUS) ||
                check(TokenType.MULTIPLY) || check(TokenType.DIVIDE) ||
                check(TokenType.MODULO) || check(TokenType.EQUAL) ||
                check(TokenType.NOT_EQUAL) || check(TokenType.LESS) ||
                check(TokenType.LESS_EQUAL) || check(TokenType.GREATER) ||
                check(TokenType.GREATER_EQUAL) || check(TokenType.AND) ||
                check(TokenType.OR) || check(TokenType.ASSIGN);
    }

    /**
     * 检查标识符是否为已知函数
     *
     * @param name 标识符名称
     * @return 是否为已知函数
     */
    public boolean isFunction(String name) {
        SymbolInfo info = symbolTable.get(name);
        return info != null && info.getType() == SymbolType.FUNCTION;
    }

    /**
     * 获取函数最大可能的参数数量
     *
     * @param name 函数名
     * @return 最大可能的参数数量
     */
    public int getMaxExpectedArgumentCount(String name) {
        SymbolInfo info = symbolTable.get(name);
        return info != null ? info.getMaxParameterCount() : 0;
    }

    /**
     * 获取函数期望的参数数量
     *
     * @param name 函数名
     * @return 期望的参数数量
     */
    public int getExpectedArgumentCount(String name) {
        return getMaxExpectedArgumentCount(name);
    }

    /**
     * 检查标识符是否为已知变量
     *
     * @param name 标识符名称
     * @return 是否为已知变量
     */
    public boolean isVariable(String name) {
        SymbolInfo info = symbolTable.get(name);
        return info != null && info.getType() == SymbolType.VARIABLE;
    }

    /**
     * 消费当前标记并前进
     */
    public void advance() {
        if (!isAtEnd()) {
            position++;
            currentToken = tokens.get(position);
        }
    }

    /**
     * 检查当前标记是否为指定类型，如果是则消费并前进
     *
     * @param type 要检查的类型
     * @return 是否匹配
     */
    public boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    /**
     * 检查当前标记是否为指定类型，如果是则消费并前进
     *
     * @param types 要检查的类型
     * @return 是否匹配
     */
    public boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前标记是否为指定类型
     *
     * @param type 要检查的类型
     * @return 是否匹配
     */
    public boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return currentToken.getType() == type;
    }

    /**
     * 检查是否已到达标记序列末尾
     *
     * @return 是否到达末尾
     */
    public boolean isAtEnd() {
        return currentToken.is(TokenType.EOF);
    }

    /**
     * 获取前一个标记
     *
     * @return 前一个标记
     */
    public Token previous() {
        return position > 0 ? tokens.get(position - 1) : tokens.get(0);
    }

    /**
     * 获取当前位置
     */
    public int mark() {
        return position;
    }

    /**
     * 回滚到记录的位置
     */
    public void rollback(int mark) {
        position = mark;
        currentToken = tokens.get(position);
    }

    /**
     * 获取当前标记
     *
     * @return 当前标记
     */
    public Token peek() {
        return currentToken;
    }

    /**
     * 获取向前看n个标记
     *
     * @param n 向前看的步数
     * @return 向前看n个标记
     */
    public Token peek(int n) {
        return position + n < tokens.size() ? tokens.get(position + n) : tokens.get(tokens.size() - 1);
    }

    /**
     * 消费当前标记，如果类型不匹配则抛出异常
     *
     * @param type    期望的类型
     * @param message 错误消息
     * @return 消费的标记
     */
    public Token consume(TokenType type, String message) {
        if (check(type)) {
            advance();
            return previous();
        }
        throw new ParseException(message, currentToken, results);
    }

    /**
     * 获取当前解析结果
     */
    public List<ParseResult> getResults() {
        return results;
    }

    /**
     * 获取符号表
     */
    public Map<String, SymbolInfo> getSymbolTable() {
        return symbolTable;
    }
}