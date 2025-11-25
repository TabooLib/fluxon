package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.LambdaExpression;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class LambdaParser {

    /**
     * Lambda 解析入口（CPS），保持在同一 trampoline 链内。
     */
    public static Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        // | 或 OR 都可作为起始
        boolean usedOrToken = parser.match(TokenType.OR);
        if (!usedOrToken) {
            parser.consume(TokenType.PIPE, "Expected '|' to start lambda expression");
        }
        // 切换当前函数名，便于记录局部变量和捕获
        String previousFunction = parser.getSymbolEnvironment().getCurrentFunction();
        String lambdaName = "__lambda$" + parser.getLambdaCounter().getAndIncrement();
        parser.getSymbolEnvironment().setCurrentFunction(lambdaName);
        // 捕获父函数局部变量索引，支持闭包
        Map<String, Integer> parentCaptures = captureParentLocals(parser, previousFunction);
        parser.pushCapture(parentCaptures);
        return parseParameters(parser, usedOrToken, parameters -> parseLambdaBody(parser, lambdaName, previousFunction, parameters, continuation));
    }

    /**
     * 捕获父函数局部变量索引，支持闭包。
     *
     * @param parser           当前解析器实例
     * @param previousFunction 上一个函数名
     * @return 父函数局部变量索引映射
     */
    private static Map<String, Integer> captureParentLocals(Parser parser, String previousFunction) {
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
        return parentCaptures;
    }

    /**
     * 解析参数列表，完成后将参数映射传给后续 continuation。
     */
    private static Trampoline<ParseResult> parseParameters(
            Parser parser,
            boolean usedOrToken,
            Function<LinkedHashMap<String, Integer>, Trampoline<ParseResult>> continuation
    ) {
        LinkedHashMap<String, Integer> parameters = new LinkedHashMap<>();
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
        return continuation.apply(parameters);
    }

    /**
     * 解析 Lambda 体，完成后将 Lambda 表达式传给后续 continuation。
     */
    private static Trampoline<ParseResult> parseLambdaBody(
            Parser parser,
            String lambdaName,
            String previousFunction,
            LinkedHashMap<String, Integer> parameters,
            Trampoline.Continuation<ParseResult> continuation
    ) {
        // 完成 lambda：收集局部变量，恢复父函数上下文与捕获栈
        Trampoline.Continuation<ParseResult> finish = body -> {
            Set<String> locals = new LinkedHashSet<>();
            Set<String> defined = parser.getSymbolEnvironment().getLocalVariables().get(lambdaName);
            if (defined != null) {
                locals.addAll(defined);
            }
            parser.getSymbolEnvironment().setCurrentFunction(previousFunction);
            parser.popCapture();
            return continuation.apply(new LambdaExpression(lambdaName, parameters, body, locals));
        };
        if (parser.match(TokenType.LEFT_BRACE)) {
            return BlockParser.parse(parser, finish);
        }
        return ExpressionParser.parse(parser, finish);
    }
}
