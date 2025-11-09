package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseException;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.expression.IndexAccessExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 后缀操作解析器
 * 处理索引访问 [] 和后缀函数调用 ()
 */
public class PostfixParser {

    /**
     * 处理后缀操作（函数调用和索引访问）
     *
     * @param parser 解析器
     * @param expr   已解析的表达式
     * @return 应用后缀操作后的表达式
     */
    public static ParseResult parsePostfixOperations(Parser parser, ParseResult expr) {
        while (true) {
            if (parser.check(TokenType.LEFT_BRACKET)) {
                // 检查 [ 是否在新行或前面是分号
                // 如果是，则不作为后缀操作，而是作为新的表达式（列表字面量）
                if (parser.isStatementBoundary()) {
                    break;
                }
                parser.advance(); // 消费 [
                // 索引访问
                expr = finishIndexAccess(parser, expr);
            }
            else if (parser.match(TokenType.LEFT_PAREN)) {
                // 后缀函数调用
                if (expr instanceof Identifier) {
                    expr = FunctionCallParser.finishCall(parser, (Identifier) expr);
                } else {
                    throw parser.createParseException("Cannot call non-identifier expression", parser.peek());
                }
            }
            else {
                break;
            }
        }
        return expr;
    }

    /**
     * 完成索引访问解析
     *
     * @param parser 解析器
     * @param target 被索引的目标表达式
     * @return IndexAccessExpression
     */
    private static ParseResult finishIndexAccess(Parser parser, ParseResult target) {
        List<ParseResult> indices = new ArrayList<>();
        // 解析索引参数（支持多个，用逗号分隔）
        if (!parser.check(TokenType.RIGHT_BRACKET)) {
            do {
                indices.add(ExpressionParser.parse(parser));
            } while (parser.match(TokenType.COMMA));
        }
        // 消费 ]
        parser.consume(TokenType.RIGHT_BRACKET, "Expected ']' after index");
        // 如果索引为空，这是一个错误（因为换行的情况已经在 parsePostfixOperations 中处理了）
        if (indices.isEmpty()) {
            throw parser.createParseException("Index access requires at least one index", parser.peek());
        }
        return new IndexAccessExpression(target, indices, -1);
    }
}

