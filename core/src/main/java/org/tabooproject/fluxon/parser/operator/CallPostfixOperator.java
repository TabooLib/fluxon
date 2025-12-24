package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.PostfixOperator;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.type.FunctionCallParser;

/**
 * 后缀函数调用运算符
 * <p>
 * 处理 {@code expr()} 语法，仅对 Identifier 类型的表达式生效。
 */
public class CallPostfixOperator implements PostfixOperator {

    @Override
    public boolean matches(Parser parser, ParseResult expr) {
        // 只有 Identifier 才能作为后缀函数调用
        return expr instanceof Identifier && parser.check(TokenType.LEFT_PAREN);
    }

    @Override
    public ParseResult parse(Parser parser, ParseResult expr) {
        parser.consume(); // 消费 (
        return FunctionCallParser.finishCall(parser, (Identifier) expr);
    }

    @Override
    public int priority() {
        return 10;
    }
}
