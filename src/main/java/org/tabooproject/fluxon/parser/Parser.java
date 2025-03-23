package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompilationPhase;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.impl.StatementParser;
import org.tabooproject.fluxon.parser.util.StringUtils;

import java.util.*;
import java.util.stream.Collector;

/**
 * Fluxon解析器
 * <p>
 * 解析优先级（从高到低）：
 * 1. Primary - 基本表达式（字面量、变量、括号表达式、when表达式、if表达式、while表达式、列表字面量、字典字面量等）
 * 2. Call - 函数调用
 * 3. Return - 返回语句（return 表达式）
 * 4. Unary - 一元表达式（!、-、await、&）
 * 5. Factor - 因子表达式（*、/、%）
 * 6. Term - 项表达式（+、-）
 * 7. Comparison - 比较表达式（>、>=、<、<=）
 * 8. Equality - 相等性表达式（==、!=）
 * 9. Range - 范围表达式（..、..<）
 * 10. LogicalAnd - 逻辑与表达式（&&）
 * 11. LogicalOr - 逻辑或表达式（||）
 * 12. Elvis - Elvis操作符（?:）
 * 13. Assignment - 赋值表达式（=、+=、-=、*=、/=）
 * <p>
 * 解析器使用递归下降解析方法，确保高优先级的操作符先于低优先级的操作符被处理。
 */
public class Parser implements CompilationPhase<List<ParseResult>> {

    // 作用域栈，用于管理不同作用域的符号
    private final Deque<SymbolScope> scopeStack = new ArrayDeque<>();

    private List<Token> tokens;
    private int position = 0;
    private Token currentToken;

    // 已经解析出的结果
    private List<ParseResult> results;

    /**
     * 创建解析器
     */
    public Parser() {
        // 初始化全局作用域
        scopeStack.push(new SymbolScope());
        initDefaultSymbols();
    }

    /**
     * 创建带有初始符号的解析器
     */
    public Parser(Map<String, SymbolInfo> initialSymbols) {
        this();
        if (initialSymbols != null) {
            for (Map.Entry<String, SymbolInfo> entry : initialSymbols.entrySet()) {
                if (entry.getValue().getType() == SymbolType.FUNCTION) {
                    defineFunction(entry.getKey(), entry.getValue());
                } else {
                    defineVariable(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * 初始化系统符号表
     */
    public void initDefaultSymbols() {
        // 预先添加一些常用函数到符号表，用于测试
        defineFunction("print", new SymbolInfo(SymbolType.FUNCTION, "print", 1));
        defineFunction("checkGrade", new SymbolInfo(SymbolType.FUNCTION, "checkGrade", 1));
        defineFunction("player", new SymbolInfo(SymbolType.FUNCTION, "player", Arrays.asList(1, 3)));
        defineFunction("fetch", new SymbolInfo(SymbolType.FUNCTION, "fetch", 1));
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
            sb.append(result.toPseudoCode());
            sb.append(";");
        }
        return sb.toString();
    }

    /**
     * 检查当前标记是否为表达式结束标记
     */
    public boolean isEndOfExpression() {
        return currentToken.getType().isEndOfExpression();
    }

    /**
     * 检查当前标记是否为操作符
     */
    public boolean isOperator() {
        return currentToken.getType().isOperator();
    }

    /**
     * 检查标识符是否为已知函数
     *
     * @param name 标识符名称
     * @return 是否为已知函数
     */
    public boolean isFunction(String name) {
        return getCurrentScope().getFunction(name) != null;
    }

    /**
     * 获取函数最大可能的参数数量
     *
     * @param name 函数名
     * @return 最大可能的参数数量
     */
    public int getMaxExpectedArgumentCount(String name) {
        SymbolInfo info = getCurrentScope().getFunction(name);
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
        return getCurrentScope().getVariable(name) != null;
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
        return currentToken.is(type);
    }

    /**
     * 检查当前标记是否为指定类型之一
     */
    public boolean check(TokenType... types) {
        if (isAtEnd()) return false;
        for (TokenType type : types) {
            if (currentToken.is(type)) {
                return true;
            }
        }
        return false;
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
     * 消费下一个标记
     */
    public Token consume() {
        if (isAtEnd()) {
            throw new ParseException("Eof", currentToken, results);
        }
        advance();
        return previous();
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
     * 进入新作用域
     */
    public void enterScope() {
        SymbolScope newScope = new SymbolScope(getCurrentScope());
        scopeStack.push(newScope);
    }

    /**
     * 退出当前作用域
     *
     * @throws IllegalStateException 如果尝试退出全局作用域
     */
    public void exitScope() {
        if (scopeStack.size() <= 1) {
            throw new IllegalStateException("Cannot exit global scope");
        }
        scopeStack.pop();
    }

    /**
     * 在当前作用域中定义函数
     *
     * @param name 函数名
     * @param info 函数信息
     */
    public void defineFunction(String name, SymbolInfo info) {
        getCurrentScope().defineFunction(name, info);
    }

    /**
     * 在当前作用域中定义变量
     *
     * @param name 变量名
     * @param info 变量信息
     */
    public void defineVariable(String name, SymbolInfo info) {
        getCurrentScope().defineVariable(name, info);
    }

    /**
     * 在当前作用域中定义变量（简化版）
     *
     * @param name 变量名
     */
    public void defineVariable(String name) {
        defineVariable(name, new SymbolInfo(SymbolType.VARIABLE, name, 0));
    }

    /**
     * 获取函数信息
     *
     * @param name 函数名
     * @return 函数信息，如果不存在则返回 null
     */
    public SymbolInfo getFunctionInfo(String name) {
        return getCurrentScope().getFunction(name);
    }

    /**
     * 获取变量信息
     *
     * @param name 变量名
     * @return 变量信息，如果不存在则返回 null
     */
    public SymbolInfo getVariableInfo(String name) {
        return getCurrentScope().getVariable(name);
    }

    /**
     * 获取当前作用域
     */
    public SymbolScope getCurrentScope() {
        return scopeStack.peek();
    }

    /**
     * 获取当前解析结果
     */
    public List<ParseResult> getResults() {
        return results;
    }

    /**
     * 获取当前作用域栈
     */
    public Deque<SymbolScope> getScopeStack() {
        return scopeStack;
    }
}