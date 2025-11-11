package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;

import java.util.*;

public class FunctionDefinitionParser {

    /**
     * 解析函数定义（无注解版本，用于向后兼容）
     *
     * @param parser  解析器
     * @param isAsync 是否为异步函数
     * @return 函数定义解析结果
     */
    public static ParseResult parse(Parser parser, boolean isAsync, boolean isPrimarySync) {
        return parse(parser, isAsync, isPrimarySync, new ArrayList<>());
    }

    /**
     * 解析函数定义
     * 关键特性：
     * 1. 允许无括号参数定义，如：def factorial n = { ... }
     * 2. 允许省略大括号
     * 3. 支持注解，如：@listener(event = "onStart") def handleStart() = { ... }
     *
     * @param parser        解析器
     * @param isAsync       是否为异步函数
     * @param isPrimarySync 是否为主线程同步函数
     * @param annotations   函数的注解列表
     * @return 函数定义解析结果
     */
    public static ParseResult parse(Parser parser, boolean isAsync, boolean isPrimarySync, List<Annotation> annotations) {
        // 解析函数名
        Token nameToken = parser.consume(TokenType.IDENTIFIER, "Expected function name");
        String functionName = nameToken.getLexeme();
        // 创建函数标记
        parser.getSymbolEnvironment().setCurrentFunction(functionName);
        // 解析参数列表（函数定义支持无括号多参数）
        LinkedHashMap<String, Integer> parameters = ParameterParser.parseParameters(
                parser,
                true,  // 函数定义支持无括号多参数
                TokenType.ASSIGN, TokenType.LEFT_BRACE  // 遇到等号或左大括号停止
        );
        // 消费可选的等于号
        parser.match(TokenType.ASSIGN);
        // 将函数添加到当前作用域
        Callable function = parser.getFunction(functionName);
        if (function != null) {
            // 函数已存在，添加新的参数数量
            List<Integer> paramCounts = new ArrayList<>(function.getParameterCounts());
            if (!paramCounts.contains(parameters.size())) {
                paramCounts.add(parameters.size());
            }
            parser.defineUserFunction(functionName, new SymbolFunction(null, functionName, paramCounts));
        } else {
            // 函数不存在，创建新条目
            parser.defineUserFunction(functionName, new SymbolFunction(null, functionName, parameters.size()));
        }

        // 解析函数体
        ParseResult body;
        // 如果有左大括号，则解析为 Block 函数体
        if (parser.match(TokenType.LEFT_BRACE)) {
            body = BlockParser.parse(parser);
        } else {
            body = ExpressionParser.parse(parser);
        }
        // 可选的分号
        parser.match(TokenType.SEMICOLON);

        // 函数局部变量
        Set<String> localVariables = parser.getSymbolEnvironment().getLocalVariables().get(functionName);
        if (localVariables == null) {
            localVariables = new HashSet<>();
        }
        // 退出函数标记
        parser.getSymbolEnvironment().setCurrentFunction(null);
        return new FunctionDefinition(functionName, parameters, body, isAsync, isPrimarySync, annotations, localVariables);
    }
}
