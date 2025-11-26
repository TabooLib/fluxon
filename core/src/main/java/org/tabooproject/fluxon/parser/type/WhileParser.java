package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolEnvironment;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.WhileExpression;

public class WhileParser {

    /**
     * 解析 While 表达式
     *
     * @return While 表达式解析结果
     */
    public static ParseResult parse(Parser parser) {
        return Trampoline.run(parse(parser, Trampoline::done));
    }

    public static Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        // 消费 WHILE 标记
        Token whileToken = parser.consume(TokenType.WHILE, "Expected 'while' before while expression");
        // 解析条件表达式
        return ExpressionParser.parse(parser, condition -> parseBody(parser, condition, continuation, whileToken));
    }

    private static Trampoline<ParseResult> parseBody(Parser parser, ParseResult condition, Trampoline.Continuation<ParseResult> continuation, Token whileToken) {
        parser.match(TokenType.THEN);
        if (parser.match(TokenType.LEFT_BRACE)) {
            SymbolEnvironment env = parser.getSymbolEnvironment();
            boolean isBreakable = env.isBreakable();
            boolean isContinuable = env.isContinuable();
            env.setBreakable(true);
            env.setContinuable(true);
            return BlockParser.parse(parser, body -> {
                env.setBreakable(isBreakable);
                env.setContinuable(isContinuable);
                return continuation.apply(parser.attachSource(new WhileExpression(condition, body), whileToken));
            });
        }
        return ExpressionParser.parse(parser, body -> continuation.apply(parser.attachSource(new WhileExpression(condition, body), whileToken)));
    }
}
