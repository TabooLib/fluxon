package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.StringInterpolation;
import org.tabooproject.fluxon.parser.expression.literal.*;

/**
 * 字面量语法宏
 * <p>
 * 合并处理所有字面量类型：
 * <ul>
 *   <li>STRING - 字符串字面量</li>
 *   <li>STRING_PART / INTERPOLATION_START - 字符串插值</li>
 *   <li>INTEGER - 整数字面量</li>
 *   <li>LONG - 长整数字面量</li>
 *   <li>FLOAT - 单精度浮点字面量</li>
 *   <li>DOUBLE - 双精度浮点字面量</li>
 *   <li>TRUE/FALSE - 布尔字面量</li>
 *   <li>NULL - 空值字面量</li>
 * </ul>
 */
public class LiteralSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        switch (parser.peek().getType()) {
            case STRING:
            case STRING_PART:
            case INTERPOLATION_START:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case TRUE:
            case FALSE:
            case NULL:
                return true;
            default:
                return false;
        }
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token token = parser.peek();
        TokenType type = token.getType();
        // 字符串插值需要特殊处理（多 token 序列）
        if (type == TokenType.STRING_PART || type == TokenType.INTERPOLATION_START) {
            ParseResult interpolation = parseStringInterpolation(parser);
            return Trampoline.more(() -> continuation.apply(interpolation));
        }
        // 其他字面量：消费 token 后直接构造
        parser.consume();
        ParseResult literal = createLiteral(token);
        return Trampoline.more(() -> continuation.apply(parser.attachSource(literal, token)));
    }

    /**
     * 根据 token 类型创建对应的字面量
     */
    private ParseResult createLiteral(Token token) {
        // @formatter:off
        switch (token.getType()) {
            case STRING:  return new StringLiteral(token.getLexeme());
            case INTEGER: return new IntLiteral((int) token.getValue());
            case LONG:    return new LongLiteral((long) token.getValue());
            case FLOAT:   return new FloatLiteral((float) token.getValue());
            case DOUBLE:  return new DoubleLiteral((double) token.getValue());
            case TRUE:    return new BooleanLiteral(true);
            case FALSE:   return new BooleanLiteral(false);
            case NULL:    return new NullLiteral();
            default:      throw new IllegalStateException("Unexpected token type: " + token.getType());
        }
        // @formatter:on
    }

    /**
     * 解析字符串插值表达式
     * <p>
     * Token 序列示例: STRING_PART("Hello ") + INTERPOLATION_START + expr + INTERPOLATION_END + STRING_PART("!")
     */
    private ParseResult parseStringInterpolation(Parser parser) {
        Token startToken = parser.peek();
        StringInterpolation.Builder builder = new StringInterpolation.Builder();
        while (!parser.isAtEnd()) {
            TokenType type = parser.peek().getType();
            if (type == TokenType.STRING_PART) {
                // 添加字符串片段
                builder.addStringPart(parser.consume().getLexeme());
            } else if (type == TokenType.INTERPOLATION_START) {
                // 消费 ${
                parser.consume();
                // 解析内部表达式
                builder.addExpression(parser.parseExpression());
                // 消费 }
                parser.consume(TokenType.INTERPOLATION_END, "Expected '}' to close string interpolation");
            } else {
                // 字符串结束
                break;
            }
        }
        StringInterpolation interpolation = builder.build();
        return parser.attachSource(interpolation, startToken);
    }

    @Override
    public int priority() {
        return 30;
    }
}
