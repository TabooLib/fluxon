package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.IndexAccessExpression;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.expression.literal.Literal;

/**
 * 赋值运算符 (=, +=, -=, *=, /=, %=)
 * <p>
 * 绑定力: 10，右结合
 */
public class AssignmentInfixOperator implements InfixOperator {

    private static final TokenType[] OPERATORS = {
            TokenType.ASSIGN,
            TokenType.PLUS_ASSIGN,
            TokenType.MINUS_ASSIGN,
            TokenType.MULTIPLY_ASSIGN,
            TokenType.DIVIDE_ASSIGN,
            TokenType.MODULO_ASSIGN
    };

    @Override
    public int bindingPower() {
        return 10;
    }

    @Override
    public boolean matches(Parser parser) {
        return parser.checkAny(OPERATORS);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, ParseResult left, Token operator, Trampoline.Continuation<ParseResult> continuation) {
        AssignmentTarget target = prepareAssignmentTarget(parser, operator.getType(), left, operator);
        // 右结合：使用相同绑定力
        return PrattParser.parseExpression(parser, bindingPower(), right -> {
            // 检查是否为常量定义（全大写标识符 + 字面量值 + 简单赋值）
            if (operator.getType() == TokenType.ASSIGN && target.expression() instanceof Identifier) {
                String name = ((Identifier) target.expression()).getValue();
                if (SymbolEnvironment.isConstantName(name) && right instanceof Literal) {
                    parser.getSymbolEnvironment().defineConstant(name, (Literal) right);
                }
            }
            return continuation.apply(parser.attachSource(new AssignExpression(target.expression(), operator, right, target.position()), operator));
        });
    }

    /**
     * 构建赋值目标信息
     */
    private static AssignmentTarget prepareAssignmentTarget(Parser parser, TokenType match, ParseResult expr, Token operator) {
        ParseResult target = expr;
        if (target instanceof ReferenceExpression) {
            target = ((ReferenceExpression) target).getIdentifier();
        }
        if (target instanceof Identifier) {
            String name = ((Identifier) target).getValue();
            // 检查是否尝试重新赋值常量
            if (parser.getSymbolEnvironment().isConstant(name)) {
                throw parser.createParseException("Cannot reassign constant: " + name, operator);
            }
            int position = parser.getSymbolEnvironment().getLocalVariable(name);
            Integer captured = parser.getCapturedIndex(name);
            if (captured != null) {
                position = captured;
            }
            if (match == TokenType.ASSIGN && captured == null && !parser.getSymbolEnvironment().hasVariable(name)) {
                parser.defineVariable(name);
                position = parser.getSymbolEnvironment().getLocalVariable(name);
            }
            return new AssignmentTarget(target, position);
        } else if (target instanceof IndexAccessExpression) {
            return new AssignmentTarget(target, -1);
        }
        throw parser.createParseException("Invalid assignment target: " + target, operator);
    }

    /**
     * 赋值目标
     */
    static class AssignmentTarget {

        private final ParseResult expression;
        private final int position;

        public AssignmentTarget(ParseResult expression, int position) {
            this.expression = expression;
            this.position = position;
        }

        public ParseResult expression() {
            return expression;
        }

        public int position() {
            return position;
        }
    }
}
