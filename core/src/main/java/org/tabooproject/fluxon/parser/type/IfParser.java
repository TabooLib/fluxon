package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.expression.IfExpression;

import java.util.Collections;

public class IfParser {

    /**
     * 解析 If 表达式
     *
     * @return If 表达式解析结果
     */
    public static ParseResult parse(Parser parser) {
        // 消费 IF 标记
        parser.consume(TokenType.IF, "Expected 'if' before if expression");

        // 解析条件
        ParseResult condition = ExpressionParser.parse(parser);

        // 尝试消费 then 标记，没有就不管
        parser.match(TokenType.THEN);

        // 解析 then 分支
        ParseResult thenBranch;
        // 如果是大括号，解析为代码块
        if (parser.match(TokenType.LEFT_BRACE)) {
            thenBranch = BlockParser.parse(parser, Collections.emptyList());
        } else {
            thenBranch = ExpressionParser.parse(parser);
        }

        // 解析 else 分支
        ParseResult elseBranch = null;
        // 如果是大括号，解析为代码块
        if (parser.match(TokenType.ELSE)) {
            if (parser.match(TokenType.LEFT_BRACE)) {
                elseBranch = BlockParser.parse(parser, Collections.emptyList());
            } else {
                elseBranch = ExpressionParser.parse(parser);
            }
        }
        return new IfExpression(condition, thenBranch, elseBranch);
    }
}
