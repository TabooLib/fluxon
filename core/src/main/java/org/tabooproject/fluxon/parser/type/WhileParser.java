package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolEnvironment;
import org.tabooproject.fluxon.parser.expression.WhileExpression;

import java.util.Collections;

public class WhileParser {

    /**
     * 解析 While 表达式
     *
     * @return While 表达式解析结果
     */
    public static ParseResult parse(Parser parser) {
        // 消费 WHILE 标记
        parser.consume(TokenType.WHILE, "Expected 'while' before while expression");
        // 解析条件表达式
        ParseResult condition = ExpressionParser.parse(parser);
        // 尝试消费 THEN 标记，如果存在
        parser.match(TokenType.THEN);
        // 解析循环体
        ParseResult body;
        // 如果有左大括号，则解析为 Block 函数体
        if (parser.match(TokenType.LEFT_BRACE)) {
            SymbolEnvironment env = parser.getSymbolEnvironment();
            // 之前的状态
            boolean isBreakable = env.isBreakable();
            boolean isContinuable = env.isContinuable();
            // 设置新的状态
            env.setBreakable(true);
            env.setContinuable(true);
            // 解析代码块
            body = BlockParser.parse(parser);
            // 恢复状态
            env.setBreakable(isBreakable);
            env.setContinuable(isContinuable);
        } else {
            body = ExpressionParser.parse(parser);
        }
        return new WhileExpression(condition, body);
    }
}
