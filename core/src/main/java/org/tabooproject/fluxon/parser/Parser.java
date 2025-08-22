package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompilationPhase;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.type.StatementParser;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;

import java.util.*;

/**
 * Fluxon解析器
 */
public class Parser implements CompilationPhase<List<ParseResult>> {

    // 符号环境
    private final SymbolEnvironment symbolEnvironment = new SymbolEnvironment();

    private List<Token> tokens;
    private int position = 0;
    private Token currentToken;

    // 当前 CompileContext
    private CompilationContext context;

    // 已经解析出的结果
    private List<ParseResult> results;

    /**
     * 执行解析
     *
     * @param context 编译上下文
     * @return 解析结果列表
     */
    @Override
    public List<ParseResult> process(CompilationContext context) {
        // 从上下文中获取词法单元序列
        this.context = context;
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
    @Nullable
    public TokenType match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return type;
            }
        }
        return null;
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
     * 抛出异常
     */
    public void error(String error) {
        throw new ParseException(error, currentToken, results);
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
     * 定义用户函数
     *
     * @param name 函数名
     * @param info 函数信息
     */
    public void defineUserFunction(String name, SymbolFunction info) {
        symbolEnvironment.defineUserFunction(name, info);
    }

    /**
     * 定义用户函数
     *
     * @param functions 函数映射
     */
    public void defineUserFunction(Map<String, Function> functions) {
        symbolEnvironment.defineUserFunctions(functions);
    }

    /**
     * 定义局部变量
     *
     * @param name 变量名
     */
    public void defineVariable(String name) {
        symbolEnvironment.defineVariable(name);
    }

    /**
     * 定义局部变量
     *
     * @param names 变量名列表
     */
    public void defineVariables(Iterable<String> names) {
        for (String name : names) {
            defineVariable(name);
        }
    }

    /**
     * 定义全局变量
     *
     * @param variables 变量映射
     */
    public void defineRootVariables(Map<String, Object> variables) {
        symbolEnvironment.defineRootVariables(variables);
    }

    /**
     * 获取函数信息
     *
     * @param name 函数名
     * @return 函数信息，如果不存在则返回 null
     */
    public Callable getFunction(String name) {
        SymbolFunction symbolFunction = symbolEnvironment.getUserFunction(name);
        if (symbolFunction != null) {
            return symbolFunction;
        }
        int i = 0;
        for (Map.Entry<String, Function> entry : FluxonRuntime.getInstance().getSystemFunctions().entrySet()) {
            if (entry.getKey().equals(name)) {
                return new FunctionPosition(entry.getValue(), i);
            }
            i++;
        }
        return null;
    }
    /**
     * 获取扩展函数信息
     *
     * @param name 函数名
     * @return 扩展函数信息，如果不存在则返回 null
     */
    public ExtensionFunctionPosition getExtensionFunction(String name) {
        Set<Map.Entry<String, Map<Class<?>, Function>>> map = FluxonRuntime.getInstance().getExtensionFunctions().entrySet();
        int i = 0;
        for (Map.Entry<String, Map<Class<?>, Function>> entry : map) {
            if (entry.getKey().equals(name)) {
                return new ExtensionFunctionPosition(entry.getValue(), i);
            }
            i++;
        }
        return null;
    }

    /**
     * 检查标识符是否为已知函数
     *
     * @param name 标识符名称
     * @return 是否为已知函数
     */
    public boolean isFunction(String name) {
        return getFunction(name) != null;
    }

    /**
     * 检查标识符是否为已知扩展函数
     *
     * @param name 标识符名称
     * @return 是否为已知扩展函数
     */
    public boolean isExtensionFunction(String name) {
        return getExtensionFunction(name) != null;
    }

    /**
     * 检查标识符是否为已知变量
     *
     * @param name 标识符名称
     * @return 是否为已知变量
     */
    public boolean hasVariable(String name) {
        return symbolEnvironment.hasVariable(name);
    }

    /**
     * 获取当前符号环境
     */
    public SymbolEnvironment getSymbolEnvironment() {
        return symbolEnvironment;
    }

    /**
     * 获取当前上下文
     */
    public CompilationContext getContext() {
        return context;
    }

    /**
     * 获取当前解析结果
     */
    public List<ParseResult> getResults() {
        return results;
    }
}