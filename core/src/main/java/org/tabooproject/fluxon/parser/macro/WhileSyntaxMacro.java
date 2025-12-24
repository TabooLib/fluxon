package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolEnvironment;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.WhileExpression;
import org.tabooproject.fluxon.parser.type.BlockParser;
import org.tabooproject.fluxon.parser.type.ExpressionParser;

/**
 * While 表达式语法宏
 */
public class WhileSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.WHILE);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        // 消费 WHILE 标记
        Token whileToken = parser.consume(TokenType.WHILE, "Expected 'while' before while expression");
        // 解析条件表达式
        return ExpressionParser.parse(parser, condition -> parseBody(parser, condition, continuation, whileToken));
    }

    private Trampoline<ParseResult> parseBody(Parser parser, ParseResult condition, Trampoline.Continuation<ParseResult> continuation, Token whileToken) {
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
