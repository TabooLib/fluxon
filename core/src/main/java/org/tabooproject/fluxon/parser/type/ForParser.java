package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.VariablePosition;
import org.tabooproject.fluxon.parser.expression.ForExpression;

import java.util.*;

public class ForParser {

    /**
     * 解析 For 表达式
     * 语法规范：
     * - 单变量形式：for <identifier> in <expression> then <...>
     * - 解构形式：for (<identifier>, <identifier>, ...) in <expression> then <...>
     *
     * @return For 表达式解析结果
     */
    public static ParseResult parse(Parser parser) {
        // 消费 FOR 标记
        parser.consume(TokenType.FOR, "Expected 'for' before for expression");
        // 进入循环作用域
        parser.enterScope(true, true);

        // 解析变量部分
        LinkedHashMap<String, VariablePosition> variables;

        // 检查是否是解构形式 (变量1, 变量2, ...)
        if (parser.match(TokenType.LEFT_PAREN)) {
            variables = parseDestructuringVariables(parser);
        } else {
            // 单变量形式
            String variable = parser.consume(TokenType.IDENTIFIER, "Expected identifier after 'for'").getLexeme();
            parser.defineVariable(variable);
            variables = new LinkedHashMap<>();
            variables.put(variable, parser.getCurrentScope().getLocalVariable(variable));
        }

        // 消费 IN 标记
        parser.consume(TokenType.IN, "Expected 'in' after identifier in for expression");

        // 解析集合表达式
        ParseResult collection = ExpressionParser.parse(parser);

        // 尝试消费 THEN 标记，如果存在
        parser.match(TokenType.THEN);

        // 解析循环体
        ParseResult body;
        // 如果有左大括号，则解析为 Block 函数体
        if (parser.match(TokenType.LEFT_BRACE)) {
            body = BlockParser.parse(parser, Collections.emptyList(), false, false);
        } else {
            body = ExpressionParser.parse(parser);
        }

        // 退出
        parser.exitScope();
        return new ForExpression(variables, collection, body, variables.size());
    }

    /**
     * 解析解构变量列表
     * 格式：(变量1, 变量2, ...)
     *
     * @param parser 解析器
     * @return 变量名列表
     */
    private static LinkedHashMap<String, VariablePosition> parseDestructuringVariables(Parser parser) {
        LinkedHashMap<String, VariablePosition> variables = new LinkedHashMap<>();
        // 解析第一个变量
        String first = parser.consume(TokenType.IDENTIFIER, "Expected identifier in destructuring declaration").getLexeme();
        parser.defineVariable(first);
        variables.put(first, parser.getCurrentScope().getLocalVariable(first));
        // 解析其余变量（以逗号分隔）
        while (parser.match(TokenType.COMMA)) {
            String name = parser.consume(TokenType.IDENTIFIER, "Expected identifier after ',' in destructuring declaration").getLexeme();
            parser.defineVariable(name);
            variables.put(name, parser.getCurrentScope().getLocalVariable(name));
        }
        // 消费右括号
        parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after destructuring variables");
        return variables;
    }
}
