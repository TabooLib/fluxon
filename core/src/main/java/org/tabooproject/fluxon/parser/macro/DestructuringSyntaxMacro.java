package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.DestructuringAssignExpression;
import org.tabooproject.fluxon.parser.type.ExpressionParser;

import java.util.LinkedHashMap;

/**
 * 解构赋值语法宏
 * <p>
 * 匹配 (var1, var2, ...) = expression 模式
 * 优先级高于普通分组表达式
 */
public class DestructuringSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        if (!parser.check(TokenType.LEFT_PAREN)) {
            return false;
        }
        // 前瞻检查 (id, ...) 模式
        // 需要检查: ( IDENTIFIER , 
        return parser.peek(1).getType() == TokenType.IDENTIFIER && parser.peek(2).getType() == TokenType.COMMA;
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token leftParen = parser.consume(); // 消费 (
        LinkedHashMap<String, Integer> variables = new LinkedHashMap<>();
        SymbolEnvironment env = parser.getSymbolEnvironment();
        // 解析第一个变量
        String first = parser.consume(TokenType.IDENTIFIER, "Expected identifier in destructuring assignment").getLexeme();
        parser.defineVariable(first);
        variables.put(first, env.getLocalVariable(first));
        // 解析其余变量（以逗号分隔）
        while (parser.match(TokenType.COMMA)) {
            String name = parser.consume(TokenType.IDENTIFIER, "Expected identifier after ',' in destructuring assignment").getLexeme();
            parser.defineVariable(name);
            variables.put(name, env.getLocalVariable(name));
        }
        // 消费右括号
        parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after destructuring variables");
        // 消费赋值操作符
        parser.consume(TokenType.ASSIGN, "Expected '=' after destructuring variables");
        // 解析右侧表达式
        return ExpressionParser.parse(parser, value -> continuation.apply(parser.attachSource(new DestructuringAssignExpression(variables, value), leftParen)));
    }

    @Override
    public int priority() {
        return 900; // 高于普通分组表达式 (50)
    }
}
