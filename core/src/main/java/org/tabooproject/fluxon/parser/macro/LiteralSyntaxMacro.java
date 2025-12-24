package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.literal.*;

/**
 * 字面量语法宏
 * <p>
 * 合并处理所有字面量类型：
 * <ul>
 *   <li>STRING - 字符串字面量</li>
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
        Token token = parser.consume();
        ParseResult literal;
        switch (token.getType()) {
            case STRING:
                literal = new StringLiteral(token.getLexeme());
                break;
            case INTEGER:
                literal = new IntLiteral((int) token.getValue());
                break;
            case LONG:
                literal = new LongLiteral((long) token.getValue());
                break;
            case FLOAT:
                literal = new FloatLiteral((float) token.getValue());
                break;
            case DOUBLE:
                literal = new DoubleLiteral((double) token.getValue());
                break;
            case TRUE:
                literal = new BooleanLiteral(true);
                break;
            case FALSE:
                literal = new BooleanLiteral(false);
                break;
            case NULL:
                literal = new NullLiteral();
                break;
            default:
                throw new IllegalStateException("Unexpected token type: " + token.getType());
        }
        return Trampoline.more(() -> continuation.apply(parser.attachSource(literal, token)));
    }

    @Override
    public int priority() {
        return 30;
    }
}
