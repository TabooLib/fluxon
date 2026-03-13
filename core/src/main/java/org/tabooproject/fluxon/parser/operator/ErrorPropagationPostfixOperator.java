package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.PostfixOperator;
import org.tabooproject.fluxon.parser.expression.ErrorPropagationExpression;

/**
 * 错误传播后缀运算符 (?)
 * <p>
 * 与三元运算符 (? expr : expr) 的区分规则：
 * 当 ? 后面的 token 在新行、或为闭合符号/分号/EOF 时视为后缀错误传播，
 * 否则视为三元运算符的一部分，由中缀解析器处理。
 *
 * @author sky
 */
public class ErrorPropagationPostfixOperator implements PostfixOperator {

    @Override
    public boolean matches(Parser parser, ParseResult expr) {
        if (!parser.check(TokenType.QUESTION)) {
            return false;
        }
        // 查看 ? 后面的 token，判断是后缀还是三元
        Token question = parser.peek();
        Token next = parser.peek(1);
        // ? 后面的 token 在新行，视为后缀
        if (next.getLine() > question.getLine()) {
            return true;
        }
        // ? 后面是闭合符号、分号或 EOF，视为后缀
        TokenType nextType = next.getType();
        return nextType == TokenType.EOF
                || nextType == TokenType.SEMICOLON
                || nextType == TokenType.RIGHT_PAREN
                || nextType == TokenType.RIGHT_BRACKET
                || nextType == TokenType.RIGHT_BRACE
                || nextType == TokenType.COMMA;
    }

    @Override
    public ParseResult parse(Parser parser, ParseResult expr) {
        parser.advance(); // 消费 ?
        return new ErrorPropagationExpression(expr);
    }

    @Override
    public int priority() {
        return 10;
    }
}
