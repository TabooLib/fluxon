package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.definition.Definitions;

import java.util.*;

public class FunctionDefinitionParser {
    
    /**
     * 解析函数定义（无注解版本，用于向后兼容）
     * @param parser 解析器
     * @param isAsync 是否为异步函数
     * @return 函数定义解析结果
     */
    public static ParseResult parse(Parser parser, boolean isAsync) {
        return parse(parser, isAsync, new ArrayList<>());
    }

    /**
     * 解析函数定义
     * 关键特性：
     * 1. 允许无括号参数定义，如：def factorial n = { ... }
     * 2. 允许省略大括号
     * 3. 支持注解，如：@listener(event = "onStart") def handleStart() = { ... }
     *
     * @param parser 解析器
     * @param isAsync 是否为异步函数
     * @param annotations 函数的注解列表
     * @return 函数定义解析结果
     */
    public static ParseResult parse(Parser parser, boolean isAsync, List<Annotation> annotations) {
        // 解析函数名
        Token nameToken = parser.consume(TokenType.IDENTIFIER, "Expected function name");
        String functionName = nameToken.getLexeme();
        // 进入循环作用域
        parser.enterScope(true, true);

        // 解析参数列表
        LinkedHashMap<String, VariablePosition> parameters = new LinkedHashMap<>();

        // 检查是否有左括号
        if (parser.match(TokenType.LEFT_PAREN)) {
            // 有括号的参数列表
            if (!parser.check(TokenType.RIGHT_PAREN)) {
                do {
                    String param = parser.consume(TokenType.IDENTIFIER, "Expected parameter name").getLexeme();
                    parser.defineVariable(param);
                    parameters.put(param, parser.getCurrentScope().getLocalVariable(param));
                } while (parser.match(TokenType.COMMA));
            }
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");
        } else {
            // 无括号的参数列表
            while (parser.match(TokenType.IDENTIFIER)) {
                String name = parser.previous().getLexeme();
                parser.defineVariable(name);
                parameters.put(name, parser.getCurrentScope().getLocalVariable(name));
            }
        }

        // 将函数添加到当前作用域
        Callable function = parser.getFunction(functionName);
        if (function != null) {
            // 函数已存在，添加新的参数数量
            List<Integer> paramCounts = new ArrayList<>(function.getParameterCounts());
            if (!paramCounts.contains(parameters.size())) {
                paramCounts.add(parameters.size());
            }
            parser.defineUserFunction(functionName, new SymbolFunction(functionName, paramCounts));
        } else {
            // 函数不存在，创建新条目
            parser.defineUserFunction(functionName, new SymbolFunction(functionName, parameters.size()));
        }

        // 消费可选的等于号
        parser.match(TokenType.ASSIGN);

        // 解析函数体
        ParseResult body;
        // 如果有左大括号，则解析为 Block 函数体
        if (parser.match(TokenType.LEFT_BRACE)) {
            body = BlockParser.parse(parser, Collections.emptyList(), false, false);
        } else {
            body = ExpressionParser.parse(parser);
        }
        // 可选的分号
        parser.match(TokenType.SEMICOLON);
        // 退出
        parser.exitScope();
        return new Definitions.FunctionDefinition(functionName, parameters, body, isAsync, annotations);
    }
}
