package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.PostfixOperator;
import org.tabooproject.fluxon.parser.expression.IndexAccessExpression;
import org.tabooproject.fluxon.parser.type.ExpressionParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 索引访问后缀运算符
 * <p>
 * 处理 {@code expr[index]} 和 {@code expr[index1, index2]} 语法。
 */
public class IndexAccessPostfixOperator implements PostfixOperator {

    @Override
    public boolean matches(Parser parser, ParseResult expr) {
        if (!parser.check(TokenType.LEFT_BRACKET)) {
            return false;
        }
        // 检查 [ 是否在新行或前面是分号
        // 如果是，则不作为后缀操作，而是作为新的表达式（列表字面量）
        return !parser.isStatementBoundary();
    }

    @Override
    public ParseResult parse(Parser parser, ParseResult expr) {
        Token leftBracket = parser.consume(); // 消费 [
        // 解析索引参数（支持多个，用逗号分隔）
        List<ParseResult> indices = new ArrayList<>();
        if (!parser.check(TokenType.RIGHT_BRACKET)) {
            do {
                indices.add(ExpressionParser.parse(parser));
            } while (parser.match(TokenType.COMMA));
        }
        // 消费 ]
        parser.consume(TokenType.RIGHT_BRACKET, "Expected ']' after index");
        // 如果索引为空，这是一个错误
        if (indices.isEmpty()) {
            throw parser.createParseException("Index access requires at least one index", parser.peek());
        }
        return parser.attachSource(new IndexAccessExpression(expr, indices, -1), leftBracket);
    }

    @Override
    public int priority() {
        return 10;
    }
}
