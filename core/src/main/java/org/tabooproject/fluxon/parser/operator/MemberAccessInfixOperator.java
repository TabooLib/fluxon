package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.MemberAccessExpression;
import org.tabooproject.fluxon.parser.type.PostfixParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 成员访问运算符 (.)
 * <p>
 * 绑定力: 115，左结合（高于 :: (110)，与属性访问相当）
 * <p>
 * 语法示例：
 * <ul>
 *   <li>{@code obj.field} - 字段访问</li>
 *   <li>{@code obj.method()} - 方法调用</li>
 *   <li>{@code obj.field.method().anotherField} - 链式调用</li>
 * </ul>
 */
public class MemberAccessInfixOperator implements InfixOperator {

    @Override
    public int bindingPower() {
        return 115; // 高于 :: (110)
    }

    @Override
    public boolean matches(Parser parser) {
        // 匹配 DOT，但排除范围操作符 (..)
        return parser.check(TokenType.DOT) && !parser.peek(1).is(TokenType.DOT);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, ParseResult left, Token operator, Trampoline.Continuation<ParseResult> continuation) {
        // 检查反射访问特性是否启用
        if (!parser.getContext().isAllowReflectionAccess()) {
            parser.error("Reflection access is not enabled. Use ctx.setEnableReflectionAccess(true) to enable the '.' operator for member access.");
        }
        // 右侧必须是标识符（成员名）
        if (!parser.check(TokenType.IDENTIFIER)) {
            parser.error("Expected member name after '.'");
        }
        Token memberToken = parser.consume(TokenType.IDENTIFIER, "Expected member name after '.'");
        String memberName = memberToken.getLexeme();
        // 检查是否为方法调用（后跟括号）
        ParseResult result;
        if (parser.match(TokenType.LEFT_PAREN)) {
            // 方法调用：obj.method(args)
            List<ParseResult> args = new ArrayList<>();
            if (!parser.check(TokenType.RIGHT_PAREN)) {
                do {
                    args.add(parser.parseExpression());
                } while (parser.match(TokenType.COMMA));
            }
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after method arguments");
            result = new MemberAccessExpression(left, memberName, args.toArray(new ParseResult[0]));
        } else {
            // 字段访问：obj.field
            result = new MemberAccessExpression(left, memberName);
        }
        // 附加源信息
        result = parser.attachSource(result, operator);
        // 处理后缀操作（如索引访问 []），链式 . 由 parseInfixLoop 自动处理
        return continuation.apply(PostfixParser.parsePostfixOperations(parser, result));
    }
}