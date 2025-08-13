package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.parser.expression.FunctionCall;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.expression.literal.StringLiteral;
import org.tabooproject.fluxon.parser.statement.Block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        else if (expr instanceof Identifier && !parser.getContext().isStrictMode()) {
            // 如果标识符后面跟着赋值操作符，保持为 Identifier，不进行函数调用解析
            // 这确保用户可以创建与函数同名的变量
            if (isAssignmentOperator(parser.peek().getType())) {
                return expr;
            }
            
            // 检查是否为已知函数
            // 只有已知函数才能进行无括号调用
            String functionName = ((Identifier) expr).getValue();
            SymbolFunction info = parser.getFunctionInfo(functionName);
            
            // 只有在上下文调用环境中才查找扩展函数
            Set<SymbolFunction> exInfo = new HashSet<>();
            if (parser.getCurrentScope().isContextCall()) {
                exInfo = parser.getExtensionFunctions(functionName);
            }

            if (info != null || !exInfo.isEmpty()) {
                // 特殊处理：如果是 0 参数函数且后面是操作符，直接调用
                if (parser.isOperator() && supportsParameterCount(info, exInfo, 0)) {
                    return new FunctionCall(expr, new ArrayList<>());
                }

                // 其他情况：只有不是操作符时才尝试收集参数
                if (!parser.isOperator()) {
                    // 获取函数的最大参数数量
                    int maxArgCount = getMaxExpectedArgumentCount(info, exInfo);
                    List<ParseResult> arguments = new ArrayList<>();

                    // 解析参数，直到达到预期的参数数量或遇到表达式结束标记
                    for (int i = 0; i < maxArgCount && !parser.isEndOfExpression(); i++) {
                        // 检查当前标记是否为标识符
                        if (parser.check(TokenType.IDENTIFIER)) {
                            String identifier = parser.peek().getLexeme();

                            // 检查标识符是否为已知函数或变量
                            // 在上下文调用环境中也检查扩展函数
                            if (parser.isFunction(identifier)
                                    || parser.isVariable(identifier)
                                    || (parser.getCurrentScope().isContextCall() && parser.isExtensionFunction(identifier))
                            ) {
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
                    if (supportsParameterCount(info, exInfo, arguments.size())) {
                        expr = new FunctionCall(expr, arguments);
                    } else {
                        // 参数数量不匹配，找到最接近的参数数量
                        Set<Integer> paramCounts = getAllParameterCounts(info, exInfo);
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
        }

        return expr;
    }

    /**
     * 获取函数最大可能的参数数量
     *
     * @param info   函数信息
     * @param exInfo 扩展函数信息集合
     * @return 最大可能的参数数量
     */
    private static int getMaxExpectedArgumentCount(SymbolFunction info, Set<SymbolFunction> exInfo) {
        int maxCount = 0;
        // 检查普通函数
        if (info != null) {
            maxCount = Math.max(maxCount, info.getMaxParameterCount());
        }
        // 检查扩展函数
        if (exInfo != null && !exInfo.isEmpty()) {
            for (SymbolFunction extFunc : exInfo) {
                maxCount = Math.max(maxCount, extFunc.getMaxParameterCount());
            }
        }
        return maxCount;
    }

    /**
     * 检查是否支持指定的参数数量
     *
     * @param info   函数信息
     * @param exInfo 扩展函数信息集合
     * @param count  参数数量
     * @return 是否支持指定的参数数量
     */
    private static boolean supportsParameterCount(SymbolFunction info, Set<SymbolFunction> exInfo, int count) {
        // 检查普通函数
        if (info != null && info.supportsParameterCount(count)) {
            return true;
        }
        // 检查扩展函数
        if (exInfo != null && !exInfo.isEmpty()) {
            for (SymbolFunction extFunc : exInfo) {
                if (extFunc.supportsParameterCount(count)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取所有可能的参数数量
     *
     * @param info   函数信息
     * @param exInfo 扩展函数信息集合
     * @return 所有可能的参数数量列表
     */
    private static Set<Integer> getAllParameterCounts(SymbolFunction info, Set<SymbolFunction> exInfo) {
        Set<Integer> allCounts = new HashSet<>();
        // 添加普通函数的参数数量
        if (info != null) {
            allCounts.addAll(info.getParameterCounts());
        }
        // 添加扩展函数的参数数量
        if (exInfo != null && !exInfo.isEmpty()) {
            for (SymbolFunction extFunc : exInfo) {
                allCounts.addAll(extFunc.getParameterCounts());
            }
        }
        return allCounts;
    }

    /**
     * 找到最接近的参数数量
     *
     * @param paramCounts 参数数量列表
     * @param actualCount 实际参数数量
     * @return 最接近的参数数量
     */
    private static int findClosestParameterCount(Set<Integer> paramCounts, int actualCount) {
        if (paramCounts.isEmpty()) {
            return 0;
        }
        // 找到最接近的参数数量
        int closestCount = paramCounts.iterator().next();
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
     * 检查是否为赋值操作符
     *
     * @param type Token类型
     * @return 是否为赋值操作符
     */
    private static boolean isAssignmentOperator(TokenType type) {
        return type == TokenType.ASSIGN ||
               type == TokenType.PLUS_ASSIGN ||
               type == TokenType.MINUS_ASSIGN ||
               type == TokenType.MULTIPLY_ASSIGN ||
               type == TokenType.DIVIDE_ASSIGN ||
               type == TokenType.MODULO_ASSIGN;
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
