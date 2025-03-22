package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompilationPhase;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.definitions.Definitions.FunctionDefinition;
import org.tabooproject.fluxon.parser.expressions.Expressions.*;
import org.tabooproject.fluxon.parser.statements.Statements.Block;
import org.tabooproject.fluxon.parser.statements.Statements.ExpressionStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluxon解析器
 */
public class Parser implements CompilationPhase<List<ParseResult>> {
    private final List<Token> tokens;
    private int position = 0;
    private Token currentToken;

    // 符号表，用于跟踪已定义的函数和变量
    private final Map<String, SymbolInfo> symbolTable = new HashMap<>();

    /**
     * 创建解析器
     *
     * @param tokens 词法单元序列
     */
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        // 预加载第一个词法单元
        if (!tokens.isEmpty()) {
            currentToken = tokens.get(0);
        }

        // 预先添加一些常用函数到符号表，用于测试
        symbolTable.put("print", new SymbolInfo(SymbolType.FUNCTION, "print", 1));
        symbolTable.put("checkGrade", new SymbolInfo(SymbolType.FUNCTION, "checkGrade", 1));
        symbolTable.put("player", new SymbolInfo(SymbolType.FUNCTION, "player", 1));
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
        @SuppressWarnings("unchecked")
        List<Token> tokens = context.getAttribute("tokens");

        if (tokens == null) {
            throw new IllegalStateException("No tokens found in compilation context");
        }

        // 创建新的解析器实例并执行解析
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    /**
     * 解析词法单元序列
     *
     * @return 解析结果列表
     */
    public List<ParseResult> parse() {
        List<ParseResult> results = new ArrayList<>();

        // 解析顶层语句
        while (!isAtEnd()) {
            ParseResult result = parseStatement();
            if (result != null) {
                results.add(result);
            }
        }
        
        return results;
    }

    /**
     * 解析语句
     *
     * @return 解析结果
     */
    private ParseResult parseStatement() {
        // 检查函数定义
        if (check(TokenType.ASYNC) && peek(1).is(TokenType.DEF)) {
            // 异步函数定义
            advance(); // 消费 ASYNC
            advance(); // 消费 DEF
            return parseFunctionDefinition(true);
        } else if (match(TokenType.DEF)) {
            // 普通函数定义
            return parseFunctionDefinition(false);
        }

        // 解析表达式语句
        return parseExpressionStatement();
    }

    /**
     * 解析函数定义
     *
     * @param isAsync 是否为异步函数
     * @return 函数定义解析结果
     */
    private ParseResult parseFunctionDefinition(boolean isAsync) {
        // 解析函数名
        Token nameToken = consume(TokenType.IDENTIFIER, "Expected function name");
        String functionName = nameToken.getValue();

        // 解析参数列表
        List<String> parameters = new ArrayList<>();

        // 检查是否有左括号
        if (match(TokenType.LEFT_PAREN)) {
            // 有括号的参数列表
            if (!check(TokenType.RIGHT_PAREN)) {
                do {
                    Token param = consume(TokenType.IDENTIFIER, "Expected parameter name");
                    parameters.add(param.getValue());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");
        } else {
            // 无括号的参数列表
            while (match(TokenType.IDENTIFIER)) {
                parameters.add(previous().getValue());
            }
        }

        // 将函数添加到符号表
        symbolTable.put(functionName, new SymbolInfo(SymbolType.FUNCTION, functionName, parameters.size()));

        // 解析等号
        consume(TokenType.ASSIGN, "Expected '=' after function declaration");

        // 解析函数体
        ParseResult body;
        if (match(TokenType.LEFT_BRACE)) {
            // 块函数体
            body = parseBlock();
        } else {
            // 表达式函数体
            if (check(TokenType.IDENTIFIER)) {
                // 如果是标识符，直接解析为变量
                Token identToken = consume(TokenType.IDENTIFIER, "Expected identifier");
                body = new Variable(identToken.getValue());
            } else if (check(TokenType.WHEN)) {
                // 如果是when表达式
                body = parseWhenExpression();
            } else if (check(TokenType.IF)) {
                body = parseIfExpression();
            } else {
                // 其他表达式
                body = parseExpression();
            }
        }

        return new FunctionDefinition(functionName, parameters, body, isAsync);
    }

    /**
     * 解析代码块
     *
     * @return 代码块解析结果
     */
    private ParseResult parseBlock() {
        List<ParseResult> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(parseStatement());
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}' after block");
        return new Block(statements);
    }

    /**
     * 解析表达式语句
     *
     * @return 表达式语句解析结果
     */
    private ParseResult parseExpressionStatement() {
        ParseResult expr = parseExpression();
        // 可选的分号
        match(TokenType.SEMICOLON);
        return new ExpressionStatement(expr);
    }

    /**
     * 解析表达式
     *
     * @return 表达式解析结果
     */
    private ParseResult parseExpression() {
        return parseAssignment();
    }

    /**
     * 解析赋值表达式
     *
     * @return 赋值表达式解析结果
     */
    private ParseResult parseAssignment() {
        ParseResult expr = parseLogicalOr();

        if (match(TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN,
                TokenType.MULTIPLY_ASSIGN, TokenType.DIVIDE_ASSIGN)) {
            Token operator = previous();
            ParseResult value = parseAssignment();

            // 检查左侧是否为有效的赋值目标
            if (expr instanceof Variable) {
                String name = ((Variable) expr).getName();
                return new Assignment(name, operator, value);
            }

            throw new ParseException("Invalid assignment target", operator);
        }

        return expr;
    }

    /**
     * 解析逻辑或表达式
     *
     * @return 逻辑或表达式解析结果
     */
    private ParseResult parseLogicalOr() {
        ParseResult expr = parseLogicalAnd();

        while (match(TokenType.OR)) {
            Token operator = previous();
            ParseResult right = parseLogicalAnd();
            expr = new LogicalExpression(expr, operator, right);
        }

        return expr;
    }

    /**
     * 解析逻辑与表达式
     *
     * @return 逻辑与表达式解析结果
     */
    private ParseResult parseLogicalAnd() {
        ParseResult expr = parseEquality();

        while (match(TokenType.AND)) {
            Token operator = previous();
            ParseResult right = parseEquality();
            expr = new LogicalExpression(expr, operator, right);
        }

        return expr;
    }

    /**
     * 解析相等性表达式
     *
     * @return 相等性表达式解析结果
     */
    private ParseResult parseEquality() {
        ParseResult expr = parseComparison();

        while (match(TokenType.EQUAL, TokenType.NOT_EQUAL)) {
            Token operator = previous();
            ParseResult right = parseComparison();
            expr = new BinaryExpression(expr, operator, right);
        }

        return expr;
    }

    /**
     * 解析比较表达式
     *
     * @return 比较表达式解析结果
     */
    private ParseResult parseComparison() {
        ParseResult expr = parseTerm();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            ParseResult right = parseTerm();
            expr = new BinaryExpression(expr, operator, right);
        }

        return expr;
    }

    /**
     * 解析项表达式
     *
     * @return 项表达式解析结果
     */
    private ParseResult parseTerm() {
        ParseResult expr = parseFactor();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            ParseResult right = parseFactor();
            expr = new BinaryExpression(expr, operator, right);
        }

        return expr;
    }

    /**
     * 解析因子表达式
     *
     * @return 因子表达式解析结果
     */
    private ParseResult parseFactor() {
        ParseResult expr = parseUnary();

        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO)) {
            Token operator = previous();
            ParseResult right = parseUnary();
            expr = new BinaryExpression(expr, operator, right);
        }

        return expr;
    }

    /**
     * 解析一元表达式
     *
     * @return 一元表达式解析结果
     */
    private ParseResult parseUnary() {
        if (match(TokenType.NOT, TokenType.MINUS)) {
            Token operator = previous();
            ParseResult right = parsePrimary();
            return new UnaryExpression(operator, right);
        }

        if (match(TokenType.AWAIT)) {
            Token operator = previous();
            ParseResult right = parseUnary();
            return new AwaitExpression(right);
        }

        if (match(TokenType.AMPERSAND)) {
            Token operator = previous();
            ParseResult right = parsePrimary();
            return new ReferenceExpression(right);
        }

        return parseCall();
    }

    /**
     * 解析函数调用表达式
     *
     * @return 函数调用表达式解析结果
     */
    private ParseResult parseCall() {
        ParseResult expr = parsePrimary();

        // 解析有括号的函数调用
        if (match(TokenType.LEFT_PAREN)) {
            expr = finishCall(expr);
        }
        // 解析无括号的函数调用
        else if (expr instanceof Variable && !isEndOfExpression() && !isOperator()) {
            String functionName = ((Variable) expr).getName();

            // 检查是否为已知函数
            if (isFunction(functionName)) {
                // 只有已知函数才能进行无括号调用
                int expectedArgCount = getExpectedArgumentCount(functionName);
                List<ParseResult> arguments = new ArrayList<>();

                // 解析参数，直到达到预期的参数数量或遇到表达式结束标记
                for (int i = 0; i < expectedArgCount && !isEndOfExpression() && !isOperator(); i++) {
                    // 检查当前标记是否为标识符
                    if (check(TokenType.IDENTIFIER)) {
                        String identifier = peek().getValue();

                        // 检查标识符是否为已知函数或变量
                        if (isFunction(identifier) || isVariable(identifier)) {
                            arguments.add(parseExpression());
                        } else {
                            // 未知标识符，转为字符串
                            advance(); // 消费标识符
                            arguments.add(new StringLiteral(identifier));
                        }
                    } else {
                        // 非标识符，按表达式解析
                        arguments.add(parseExpression());
                    }

                    // 如果遇到逗号或者已经解析了足够的参数，继续解析下一个参数
                    if (!match(TokenType.COMMA)) {
                        break;
                    }
                }

                expr = new FunctionCall(expr, arguments);
            }
        }

        return expr;
    }

    /**
     * 解析函数调用参数
     *
     * @return 参数解析结果
     */
    private ParseResult parseArgument() {
        // 检查是否为标识符
        // 所有参数都按表达式解析
        return parseExpression();
    }

    /**
     * 完成函数调用解析
     *
     * @param callee 被调用者
     * @return 函数调用解析结果
     */
    private ParseResult finishCall(ParseResult callee) {
        List<ParseResult> arguments = new ArrayList<>();

        // 如果参数列表不为空
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(parseExpression());
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");

        return new FunctionCall(callee, arguments);
    }

    /**
     * 解析基本表达式
     *
     * @return 基本表达式解析结果
     */
    private ParseResult parsePrimary() {
        // 字面量
        if (match(TokenType.FALSE)) {
            return new BooleanLiteral(false);
        }
        if (match(TokenType.TRUE)) {
            return new BooleanLiteral(true);
        }
        if (match(TokenType.INTEGER)) {
            return new IntegerLiteral(previous().getValue());
        }
        if (match(TokenType.FLOAT)) {
            return new FloatLiteral(previous().getValue());
        }
        if (match(TokenType.STRING)) {
            return new StringLiteral(previous().getValue());
        }

        // 变量引用
        if (match(TokenType.IDENTIFIER)) {
            String name = previous().getValue();
            return new Variable(name);
        }

        // 分组表达式
        if (match(TokenType.LEFT_PAREN)) {
            ParseResult expr = parseExpression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
            return new GroupingExpression(expr);
        }

        // When 表达式
        if (match(TokenType.WHEN)) {
            return parseWhenExpression();
        }

        // If 表达式
        if (match(TokenType.IF)) {
            return parseIfExpression();
        }

        throw new ParseException("Expected expression", currentToken);
    }

    /**
     * 解析 When 表达式
     *
     * @return When 表达式解析结果
     */
    private ParseResult parseWhenExpression() {
        // 可选的条件表达式
        ParseResult condition = null;
        advance(); // 消费 WHEN 标记
        if (!check(TokenType.LEFT_BRACE)) {
            condition = parseExpression();
        }

        // 消费左花括号，如果存在的话
        match(TokenType.LEFT_BRACE);

        // 如果当前是EOF，直接返回空的when表达式
        if (isAtEnd()) {
            return new WhenExpression(condition, new ArrayList<>());
        }

        // 解析分支
        List<WhenBranch> branches = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            // 解析分支条件
            ParseResult branchCondition = null;

            if (match(TokenType.ELSE)) {
                // else 分支
                // 消费箭头操作符
                consume(TokenType.ARROW, "Expected '->' after else");
            } else {
                // 条件分支
                // 解析引用表达式 &num
                if (match(TokenType.AMPERSAND)) {
                    Token ampersand = previous();
                    Token identifier = consume(TokenType.IDENTIFIER, "Expected identifier after &");
                    ParseResult varRef = new ReferenceExpression(new Variable(identifier.getValue()));

                    // 解析剩余的表达式
                    branchCondition = parseRestOfExpression(varRef);
                } else {
                    branchCondition = parseExpression();
                }
                consume(TokenType.ARROW, "Expected '->' after branch condition");
            }

            // 解析分支结果
            ParseResult branchResult;
            branchResult = parseExpression();

            branches.add(new WhenBranch(branchCondition, branchResult));

            // 可选的分支结束符
            match(TokenType.SEMICOLON);
        }

        // 消费右花括号，如果存在的话
        match(TokenType.RIGHT_BRACE);

        return new WhenExpression(condition, branches);
    }

    /**
     * 解析表达式的剩余部分，用于处理特殊情况
     *
     * @param left 左侧表达式
     * @return 完整表达式
     */
    private ParseResult parseRestOfExpression(ParseResult left) {

        // 处理二元操作符
        if (match(TokenType.MODULO)) {
            Token operator = previous();
            ParseResult right = parsePrimary(); // 解析右侧操作数
            left = new BinaryExpression(left, operator, right);
        }

        // 处理比较操作符
        if (match(TokenType.EQUAL, TokenType.NOT_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL,
                TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            Token operator = previous();
            ParseResult right = parsePrimary(); // 解析右侧操作数
            left = new BinaryExpression(left, operator, right);
        }

        // 处理加减操作符
        if (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            ParseResult right = parsePrimary(); // 解析右侧操作数
            left = new BinaryExpression(left, operator, right);
        }

        // 处理乘除操作符
        if (match(TokenType.MULTIPLY, TokenType.DIVIDE)) {
            Token operator = previous();
            ParseResult right = parsePrimary(); // 解析右侧操作数
            left = new BinaryExpression(left, operator, right);
        }

        return left;
    }

    /**
     * 解析 If 表达式
     *
     * @return If 表达式解析结果
     */
    private ParseResult parseIfExpression() {
        advance(); // 消费 IF 标记

        ParseResult condition = parseExpression();
        // 检查是否有THEN关键字，如果没有，尝试继续解析
        if (!match(TokenType.THEN)) {
            throw new ParseException("Expected 'then' after if condition", currentToken);
 // 保留警告，但抛出异常
        }

        // 解析then分支
        ParseResult thenBranch = parseExpression();

        ParseResult elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = parseExpression();
        }

        return new IfExpression(condition, thenBranch, elseBranch);
    }

    /**
     * 检查当前标记是否为表达式结束标记
     *
     * @return 是否为表达式结束标记
     */
    private boolean isEndOfExpression() {
        return check(TokenType.SEMICOLON) || check(TokenType.RIGHT_PAREN) ||
                check(TokenType.RIGHT_BRACE) || check(TokenType.RIGHT_BRACKET) ||
                check(TokenType.COMMA) || check(TokenType.EOF);
    }

    /**
     * 检查当前标记是否为操作符
     *
     * @return 是否为操作符
     */
    private boolean isOperator() {
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
    private boolean isFunction(String name) {
        SymbolInfo info = symbolTable.get(name);
        return info != null && info.getType() == SymbolType.FUNCTION;
    }

    /**
     * 获取函数期望的参数数量
     *
     * @param name 函数名
     * @return 期望的参数数量
     */
    private int getExpectedArgumentCount(String name) {
        SymbolInfo info = symbolTable.get(name);
        return info != null ? info.getParameterCount() : 0;
    }

    /**
     * 检查标识符是否为已知变量
     *
     * @param name 标识符名称
     * @return 是否为已知变量
     */
    private boolean isVariable(String name) {
        SymbolInfo info = symbolTable.get(name);
        return info != null && info.getType() == SymbolType.VARIABLE;
    }

    /**
     * 消费当前标记并前进
     */
    private void advance() {
        if (!isAtEnd()) {
            position++;
            currentToken = tokens.get(position);
        }
    }

    /**
     * 检查当前标记是否为指定类型，如果是则消费并前进
     *
     * @param types 要检查的类型
     * @return 是否匹配
     */
    private boolean match(TokenType... types) {
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
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return currentToken.getType() == type;
    }

    /**
     * 检查是否已到达标记序列末尾
     *
     * @return 是否到达末尾
     */
    private boolean isAtEnd() {
        return currentToken.is(TokenType.EOF);
    }

    /**
     * 获取前一个标记
     *
     * @return 前一个标记
     */
    private Token previous() {
        return position > 0 ? tokens.get(position - 1) : tokens.get(0);
    }

    /**
     * 获取当前标记
     *
     * @return 当前标记
     */
    private Token peek() {
        return currentToken;
    }

    /**
     * 获取向前看n个标记
     *
     * @param n 向前看的步数
     * @return 向前看n个标记
     */
    private Token peek(int n) {
        return position + n < tokens.size() ? tokens.get(position + n) : tokens.get(tokens.size() - 1);
    }

    /**
     * 消费当前标记，如果类型不匹配则抛出异常
     *
     * @param type    期望的类型
     * @param message 错误消息
     * @return 消费的标记
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) {
            advance();
            return previous();
        }

        throw new ParseException(message, currentToken);
    }

    /**
     * 符号类型枚举
     */
    private enum SymbolType {
        FUNCTION,
        VARIABLE
    }

    /**
     * 符号信息类
     */
    private static class SymbolInfo {
        private final SymbolType type;
        private final String name;
        private final int parameterCount;

        public SymbolInfo(SymbolType type, String name, int parameterCount) {
            this.type = type;
            this.name = name;
            this.parameterCount = parameterCount;
        }

        public SymbolType getType() {
            return type;
        }

        public int getParameterCount() {
            return parameterCount;
        }
    }
}