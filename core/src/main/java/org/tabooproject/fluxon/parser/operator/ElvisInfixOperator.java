package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.InfixOperator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.ElvisExpression;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.parser.type.StatementParser;

/**
 * Elvis 运算符 (?:)
 * <p>
 * 绑定力: 20，右结合
 * 右侧支持块表达式或普通表达式
 */
public class ElvisInfixOperator implements InfixOperator {

    @Override
    public int bindingPower() {
        return 20;
    }

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.QUESTION_COLON);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, ParseResult left, Token operator, Trampoline.Continuation<ParseResult> continuation) {
        // 右侧支持子语句（块或表达式）
        return StatementParser.parseSubToExpr(parser, normalized -> continuation.apply(parser.attachSource(new ElvisExpression(left, normalized), operator)));
    }
}