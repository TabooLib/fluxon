package org.tabooproject.fluxon.parser.impl;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolInfo;
import org.tabooproject.fluxon.parser.SymbolType;
import org.tabooproject.fluxon.parser.definitions.Definitions;
import org.tabooproject.fluxon.parser.expressions.Identifier;

import java.util.ArrayList;
import java.util.List;

public class FunctionDefinitionParser {

    /**
     * 解析函数定义
     * 关键特性：
     * 1. 允许无括号参数定义，如：def factorial n = { ... }
     * 2. 允许省略大括号
     *
     * @param isAsync 是否为异步函数
     * @return 函数定义解析结果
     */
    public static ParseResult parse(Parser parser, boolean isAsync) {
        // 解析函数名
        Token nameToken = parser.consume(TokenType.IDENTIFIER, "Expected function name");
        String functionName = nameToken.getLexeme();

        // 解析参数列表
        List<String> parameters = new ArrayList<>();

        // 检查是否有左括号
        if (parser.match(TokenType.LEFT_PAREN)) {
            // 有括号的参数列表
            if (!parser.check(TokenType.RIGHT_PAREN)) {
                do {
                    Token param = parser.consume(TokenType.IDENTIFIER, "Expected parameter name");
                    parameters.add(param.getLexeme());
                } while (parser.match(TokenType.COMMA));
            }
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");
        } else {
            // 无括号的参数列表
            while (parser.match(TokenType.IDENTIFIER)) {
                parameters.add(parser.previous().getLexeme());
            }
        }

        // 将函数添加到当前作用域
        SymbolInfo existingInfo = parser.getFunctionInfo(functionName);
        if (existingInfo != null && existingInfo.getType() == SymbolType.FUNCTION) {
            // 函数已存在，添加新的参数数量
            List<Integer> paramCounts = new ArrayList<>(existingInfo.getParameterCounts());
            if (!paramCounts.contains(parameters.size())) {
                paramCounts.add(parameters.size());
            }
            parser.defineFunction(functionName, new SymbolInfo(SymbolType.FUNCTION, functionName, paramCounts));
        } else {
            // 函数不存在，创建新条目
            parser.defineFunction(functionName, new SymbolInfo(SymbolType.FUNCTION, functionName, parameters.size()));
        }

        // 进入函数作用域
        parser.enterScope();

        // 将参数添加到函数作用域
        for (String param : parameters) {
            parser.defineVariable(param);
        }

        // 解析等号
        parser.consume(TokenType.ASSIGN, "Expected '=' after function declaration");

        // 解析函数体
        ParseResult body;
        
        // 如果有左大括号，则解析为 Block 函数体
        if (parser.match(TokenType.LEFT_BRACE)) {
            body = BlockParser.parse(parser);
        } else {
            // 如果是标识符，直接解析为变量
            if (parser.check(TokenType.IDENTIFIER)) {
                Token identToken = parser.consume(TokenType.IDENTIFIER, "Expected identifier");
                body = new Identifier(identToken.getLexeme());
            }
            // When 结构
            else if (parser.check(TokenType.WHEN)) {
                body = WhenParser.parse(parser);
            }
            // If 结构
            else if (parser.check(TokenType.IF)) {
                body = IfParser.parse(parser);
            }
            // 其他
            else {
                body = ExpressionParser.parse(parser);
            }
        }
        // 退出函数作用域
        parser.exitScope();
        return new Definitions.FunctionDefinition(functionName, parameters, body, isAsync);
    }
}
