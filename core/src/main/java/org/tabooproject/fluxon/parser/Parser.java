package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompilationPhase;
import org.tabooproject.fluxon.parser.error.FunctionNotFoundException;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.parser.type.FunctionInfo;
import org.tabooproject.fluxon.parser.type.ImportParser;
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

    // 导入
    private List<String> imports;
    // 已经解析出的结果
    private List<ParseResult> results;
    // 待解析的函数调用列表
    private List<PendingFunctionCall> pendingCalls;
    // 错误收集列表
    private List<ParseException> errors;
    // 捕获外部变量的栈
    private final Deque<Map<String, Integer>> captureStack = new ArrayDeque<>();

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
        this.imports = new ArrayList<>(context.getPackageAutoImport());
        this.results = new ArrayList<>();
        this.pendingCalls = new ArrayList<>();
        this.errors = new ArrayList<>();

        // 解析导入
        try {
            ImportParser.parse(this);
        } catch (ParseException ex) {
            recordError(ex);
            synchronize();
        }

        // 解析顶层语句
        while (!isAtEnd()) {
            try {
                results.add(StatementParser.parseTopLevel(this));
            } catch (ParseException ex) {
                recordError(ex);
                synchronize();
            }
        }

        // 解析所有待解析的函数调用
        resolvePendingCalls();

        // 将错误列表存入上下文
        context.setAttribute("parseErrors", errors);
        context.setAttribute("results", results);

        // 如果有错误，抛出异常
        if (!errors.isEmpty()) {
            if (errors.size() == 1) {
                // 只有一个错误，直接抛出
                throw errors.get(0);
            } else {
                // 多个错误，抛出多重异常
                throw new MultipleParseException(errors);
            }
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
     * 检查当前 token 是否在语句边界
     * 通过以下条件判断：
     * 1. 当前 token 在新行（行号大于前一个 token）
     * 2. 前一个 token 是分号
     *
     * @return 是否在语句边界
     */
    public boolean isStatementBoundary() {
        // 检查是否在新行
        if (currentToken.getLine() > previous().getLine()) {
            return true;
        }
        // 检查前一个 token 是否是分号
        return previous().getType() == TokenType.SEMICOLON;
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

    public void pushCapture(Map<String, Integer> captures) {
        captureStack.push(captures);
    }

    public void popCapture() {
        if (!captureStack.isEmpty()) {
            captureStack.pop();
        }
    }

    @Nullable
    public Integer getCapturedIndex(String name) {
        if (captureStack.isEmpty()) {
            return null;
        }
        for (Map<String, Integer> map : captureStack) {
            if (map.containsKey(name)) {
                return map.get(name);
            }
        }
        return null;
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
        throw createParseException(error, currentToken);
    }

    /**
     * 创建带源码摘录的解析异常
     *
     * @param reason 错误原因
     * @param token  相关的词法单元
     * @return 解析异常
     */
    public ParseException createParseException(String reason, Token token) {
        Token highlightToken = selectHighlightToken(token);
        SourceExcerpt excerpt = SourceExcerpt.from(context, highlightToken);
        return new ParseException(reason, highlightToken, results, excerpt);
    }

    /**
     * 消费下一个标记
     */
    public Token consume() {
        if (isAtEnd()) {
            throw createParseException("Eof", currentToken);
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
        throw createParseException(message, currentToken);
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
    @Nullable
    public Callable getFunction(String name) {
        SymbolFunction symbolFunction = symbolEnvironment.getUserFunction(name);
        if (symbolFunction != null) {
            return symbolFunction;
        }
        int i = 0;
        for (Map.Entry<String, Function> entry : FluxonRuntime.getInstance().getSystemFunctions().entrySet()) {
            if (entry.getKey().equals(name)) {
                String namespace = entry.getValue().getNamespace();
                if (namespace == null || imports.contains(namespace)) {
                    return new FunctionPosition(entry.getValue(), i);
                }
                return null;
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
                Map<Class<?>, Function> typeMap = entry.getValue();
                Map<Class<?>, Function> filteredTypeMap = new HashMap<>();
                for (Map.Entry<Class<?>, Function> typeEntry : typeMap.entrySet()) {
                    String namespace = typeEntry.getValue().getNamespace();
                    if (namespace == null || imports.contains(namespace)) {
                        filteredTypeMap.put(typeEntry.getKey(), typeEntry.getValue());
                    }
                }
                if (!filteredTypeMap.isEmpty()) {
                    return new ExtensionFunctionPosition(filteredTypeMap, i);
                }
                return null;
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
     * 获取当前导入列表
     */
    public List<String> getImports() {
        return imports;
    }

    /**
     * 获取当前解析结果
     */
    public List<ParseResult> getResults() {
        return results;
    }

    /**
     * 注册待解析的函数调用
     *
     * @param expression 函数调用表达式
     * @param nameToken  函数名 token
     */
    public void registerPendingCall(FunctionCallExpression expression, Token nameToken) {
        pendingCalls.add(new PendingFunctionCall(expression, nameToken));
    }

    /**
     * 解析所有待解析的函数调用
     * 在所有函数定义都已解析后调用
     */
    private void resolvePendingCalls() {
        for (PendingFunctionCall pending : pendingCalls) {
            String name = pending.getFunctionName();
            // 查找函数信息
            FunctionInfo funcInfo = FunctionInfo.lookup(this, name);
            // 如果函数不存在，抛出异常
            if (!funcInfo.isFound()) {
                // 恢复到调用位置以提供准确的错误信息
                Token token = pending.getNameToken();
                SourceExcerpt excerpt = SourceExcerpt.from(context, token);
                throw new FunctionNotFoundException(name, token, excerpt);
            }
            // 设置函数位置信息
            FunctionCallExpression expr = pending.getExpression();
            expr.setPosition(funcInfo.getPosition());
            expr.setExtensionPosition(funcInfo.getExtensionPosition());
        }
    }

    /**
     * 记录解析错误
     *
     * @param ex 解析异常
     */
    private void recordError(ParseException ex) {
        errors.add(ex);
    }

    /**
     * 同步到下一个语句边界
     * 用于错误恢复，跳过当前错误语句并找到下一个安全的解析点
     * <p>
     * 采用 panic-mode 恢复策略：
     * 1. 跟踪大括号深度，确保完整跳过嵌套块
     * 2. 在深度为 0 时遇到分号或顶层关键字（def/fun/val/var等）立即停止
     * 3. 使用 lastPosition 哨兵防止无限循环
     * 4. 在遇到左大括号时增加深度，右大括号时减少深度
     * 5. 只在大括号平衡后才认为到达安全恢复点
     */
    private void synchronize() {
        if (isAtEnd()) {
            return;
        }
        int braceDepth = 0;
        int lastPosition = -1;
        while (!isAtEnd()) {
            // 防止无限循环：如果位置没变化则强制前进
            if (position == lastPosition) {
                advance();
                continue;
            }
            lastPosition = position;
            TokenType type = currentToken.getType();
            // 遇到分号：语句分隔符，消费后停止
            if (type == TokenType.SEMICOLON) {
                advance();
                return;
            }
            // 在顶层遇到恢复点关键字：停止同步
            if (braceDepth == 0 && type.isTopLevelRecoveryPoint()) {
                return;
            }
            // 跟踪大括号深度
            if (type == TokenType.LEFT_BRACE) {
                braceDepth++;
                advance();
                continue;
            }
            if (type == TokenType.RIGHT_BRACE) {
                if (braceDepth > 0) {
                    braceDepth--;
                }
                advance();
                continue;
            }
            advance();
        }
    }

    private Token selectHighlightToken(Token token) {
        if (token == null) {
            return null;
        }
        if (shouldFallbackToPrevious(token) && position > 0) {
            Token previous = previous();
            if (previous != null && previous.getLine() > 0) {
                return previous;
            }
        }
        return token;
    }

    private boolean shouldFallbackToPrevious(Token token) {
        TokenType type = token.getType();
        switch (type) {
            case RIGHT_BRACE:
            case RIGHT_PAREN:
            case RIGHT_BRACKET:
            case SEMICOLON:
            case EOF:
                return true;
            default:
                return token.getLexeme().isEmpty();
        }
    }
}
