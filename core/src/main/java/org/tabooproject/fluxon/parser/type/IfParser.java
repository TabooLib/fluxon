package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.IfExpression;

import java.util.Collections;

public class IfParser {

    /**
     * 解析 If 表达式
     *
     * @return If 表达式解析结果
     */
    @SuppressWarnings("DuplicatedCode")
    public static ParseResult parse(Parser parser) {
        return Trampoline.run(parse(parser, Trampoline::done));
    }

    @SuppressWarnings("DuplicatedCode")
    public static Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        // 消费 IF 标记
        parser.consume(TokenType.IF, "Expected 'if' before if expression");
        return ExpressionParser.parse(parser, condition -> parseThenBranch(parser, condition, continuation));
    }

    private static Trampoline<ParseResult> parseThenBranch(Parser parser, ParseResult condition, Trampoline.Continuation<ParseResult> continuation) {
        parser.match(TokenType.THEN);
        return parseBranch(parser, thenBranch -> parseElseBranch(parser, condition, thenBranch, continuation));
    }

    private static Trampoline<ParseResult> parseElseBranch(Parser parser, ParseResult condition, ParseResult thenBranch, Trampoline.Continuation<ParseResult> continuation) {
        if (parser.match(TokenType.ELSE)) {
            if (parser.match(TokenType.LEFT_BRACE)) {
                return BlockParser.parse(parser, elseBranch -> continuation.apply(new IfExpression(condition, thenBranch, elseBranch)));
            } else {
                return ExpressionParser.parse(parser, elseBranch -> continuation.apply(new IfExpression(condition, thenBranch, elseBranch)));
            }
        }
        return Trampoline.more(() -> continuation.apply(new IfExpression(condition, thenBranch, null)));
    }

    private static Trampoline<ParseResult> parseBranch(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        if (parser.match(TokenType.LEFT_BRACE)) {
            return BlockParser.parse(parser, continuation);
        }
        return ExpressionParser.parse(parser, continuation);
    }
}
