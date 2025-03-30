package org.tabooproject.fluxon.parser.impl;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.parser.expressions.FunctionCall;
import org.tabooproject.fluxon.parser.expressions.Identifier;
import org.tabooproject.fluxon.parser.expressions.StringLiteral;
import org.tabooproject.fluxon.parser.statements.Block;

import java.util.ArrayList;
import java.util.List;

public class FunctionCallParser {

    /**
     * 解析函数调用表达式
     *
     * @return 函数调用表达式解析结果
     */
    public static ParseResult parse(Parser parser) {
        ParseResult expr = ExpressionParser.parsePrimary(parser);

        // 解析有括号的函数调用
        if (parser.match(TokenType.LEFT_PAREN)) {
            expr = finishCall(parser, expr);
        }
        // 解析无括号的函数调用
        else if (expr instanceof Identifier && !parser.isEndOfExpression() && !parser.isOperator()) {
            String functionName = ((Identifier) expr).getValue();

            // 检查是否为已知函数
            // 只有已知函数才能进行无括号调用
            SymbolFunction info = parser.getFunctionInfo(functionName);
            if (info != null) {
                // 获取函数的最大参数数量
                int maxArgCount = parser.getMaxExpectedArgumentCount(functionName);
                List<ParseResult> arguments = new ArrayList<>();

                // 解析参数，直到达到预期的参数数量或遇到表达式结束标记
                for (int i = 0; i < maxArgCount && !parser.isEndOfExpression() && !parser.isOperator(); i++) {
                    // 检查当前标记是否为标识符
                    if (parser.check(TokenType.IDENTIFIER)) {
                        String identifier = parser.peek().getLexeme();

                        // 检查标识符是否为已知函数或变量
                        if (parser.isFunction(identifier) || parser.isVariable(identifier)) {
                            arguments.add(ExpressionParser.parse(parser));
                        } else {
                            // 未知标识符，转为字符串
                            parser.advance(); // 消费标识符
                            arguments.add(new StringLiteral(identifier));
                        }
                    } else {
                        // 非标识符，按表达式解析
                        arguments.add(ExpressionParser.parse(parser));
                    }

                    // 如果遇到分号就跳出
                    if (parser.match(TokenType.SEMICOLON)) {
                        break;
                    }
                }

                // 检查解析到的参数数量是否有效
                if (info.supportsParameterCount(arguments.size())) {
                    expr = new FunctionCall(expr, arguments);
                } else {
                    // 参数数量不匹配，找到最接近的参数数量
                    List<Integer> paramCounts = info.getParameterCounts();
                    int closestCount = findClosestParameterCount(paramCounts, arguments.size());
                    List<ParseResult> block = new ArrayList<>();
                    // 使用足额的参数
                    block.add(new FunctionCall(expr, arguments.subList(0, closestCount)));
                    // 和剩下的参数打包成代码块，避免回滚二次解析
                    block.addAll(arguments.subList(closestCount, arguments.size()));
                    expr = new Block("ipc", block);
                }
            }
        }

        return expr;
    }

    /**
     * 找到最接近的参数数量
     *
     * @param paramCounts 参数数量列表
     * @param actualCount 实际参数数量
     * @return 最接近的参数数量
     */
    private static int findClosestParameterCount(List<Integer> paramCounts, int actualCount) {
        if (paramCounts.isEmpty()) {
            return 0;
        }

        // 找到最接近的参数数量
        int closestCount = paramCounts.get(0);
        int minDiff = Math.abs(closestCount - actualCount);

        for (int count : paramCounts) {
            int diff = Math.abs(count - actualCount);
            if (diff < minDiff) {
                minDiff = diff;
                closestCount = count;
            }
        }
        return closestCount;
    }

    /**
     * 完成函数调用解析
     *
     * @param callee 被调用者
     * @return 函数调用解析结果
     */
    private static ParseResult finishCall(Parser parser, ParseResult callee) {
        List<ParseResult> arguments = new ArrayList<>();
        // 如果参数列表不为空
        if (!parser.check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(ExpressionParser.parse(parser));
            } while (parser.match(TokenType.COMMA));
        }
        parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
        return new FunctionCall(callee, arguments);
    }
}
