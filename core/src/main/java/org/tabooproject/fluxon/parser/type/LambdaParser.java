package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.expression.LambdaExpression;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lambda 语法解析
 */
public class LambdaParser {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public static LambdaExpression parse(Parser parser) {
        boolean usedOrToken = parser.match(TokenType.OR);
        if (!usedOrToken) {
            parser.consume(TokenType.PIPE, "Expected '|' to start lambda expression");
        }

        String previousFunction = parser.getSymbolEnvironment().getCurrentFunction();
        String lambdaName = "__lambda$" + COUNTER.getAndIncrement();
        parser.getSymbolEnvironment().setCurrentFunction(lambdaName);

        // 捕获父函数的局部变量索引
        Map<String, Integer> parentCaptures = new LinkedHashMap<>();
        if (previousFunction != null) {
            Set<String> parentLocals = parser.getSymbolEnvironment().getLocalVariables().get(previousFunction);
            if (parentLocals != null) {
                int idx = 0;
                for (String name : parentLocals) {
                    parentCaptures.put(name, idx++);
                }
            }
        }
        parser.pushCapture(parentCaptures);

        LinkedHashMap<String, Integer> parameters = new LinkedHashMap<>();
        // 解析参数列表，直到遇到结束的 |
        if (!usedOrToken && !parser.check(TokenType.PIPE)) {
            do {
                String paramName = parser.consume(TokenType.IDENTIFIER, "Expected parameter name in lambda").getLexeme();
                parser.defineVariable(paramName);
                parameters.put(paramName, parser.getSymbolEnvironment().getLocalVariable(paramName));
            } while (parser.match(TokenType.COMMA) && !parser.check(TokenType.PIPE));
        }
        if (!usedOrToken) {
            parser.consume(TokenType.PIPE, "Expected '|' after lambda parameters");
        }

        ParseResult body;
        if (parser.match(TokenType.LEFT_BRACE)) {
            body = BlockParser.parse(parser);
        } else {
            body = ExpressionParser.parse(parser);
        }

        Set<String> locals = new LinkedHashSet<>();
        Set<String> defined = parser.getSymbolEnvironment().getLocalVariables().get(lambdaName);
        if (defined != null) {
            locals.addAll(defined);
        }
        parser.getSymbolEnvironment().setCurrentFunction(previousFunction);
        parser.popCapture();
        return new LambdaExpression(lambdaName, parameters, body, locals);
    }
}
