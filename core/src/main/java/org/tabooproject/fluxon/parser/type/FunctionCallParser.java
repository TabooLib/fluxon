package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.expression.literal.StringLiteral;
import org.tabooproject.fluxon.parser.statement.Block;
import org.tabooproject.fluxon.runtime.Function;

import java.util.*;

public class FunctionCallParser {

    /**
     * 解析函数调用表达式
     *
     * @return 函数调用表达式解析结果
     */
    public static ParseResult parse(Parser parser) {
        ParseResult name = ExpressionParser.parsePrimary(parser);

        if (name instanceof Identifier) {
            // 有括号
            if (parser.match(TokenType.LEFT_PAREN)) {
                return finishCall(parser, (Identifier) name);
            }
            // 无括号
            else if (parser.getContext().isAllowKetherStyleCall()) {
                // region
                // 如果标识符后面跟着赋值操作符，保持为 Identifier，不进行函数调用解析
                // 这确保用户可以创建与函数同名的变量
                if (isAssignmentOperator(parser.peek().getType())) {
                    return name;
                }

                // 检查是否为已知函数
                // 只有已知函数才能进行无括号调用
                String functionName = ((Identifier) name).getValue();
                Callable info = parser.getFunction(functionName);
                FunctionPosition position = info instanceof FunctionPosition ? (FunctionPosition) info : null;

                // 只有在上下文调用环境中才查找扩展函数
                ExtensionFunctionPosition exInfo = null;
                if (parser.getSymbolEnvironment().isContextCall()) {
                    exInfo = parser.getExtensionFunction(functionName);
                }

                if (info != null || exInfo != null) {
                    // 特殊处理：如果是 0 参数函数且后面是操作符，直接调用
                    if (parser.isOperator() && supportsParameterCount(info, exInfo, 0)) {
                        return new FunctionCallExpression(((Identifier) name).getValue(), new ArrayList<>(), position, exInfo);
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
                                if (parser.isFunction(identifier) || parser.hasVariable(identifier)
                                        // 如果在上下文调用环境中也检查扩展函数
                                        || (parser.getSymbolEnvironment().isContextCall() && parser.isExtensionFunction(identifier))
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
                            return new FunctionCallExpression(((Identifier) name).getValue(), arguments, position, exInfo);
                        } else {
                            // 参数数量不匹配，找到最接近的参数数量
                            Set<Integer> paramCounts = getAllParameterCounts(info, exInfo);
                            int closestCount = findClosestParameterCount(paramCounts, arguments.size());
                            List<ParseResult> block = new ArrayList<>();
                            // 使用足额的参数
                            block.add(new FunctionCallExpression(((Identifier) name).getValue(), arguments.subList(0, closestCount), position, exInfo));
                            // 和剩下的参数打包成代码块，避免回滚二次解析
                            block.addAll(arguments.subList(closestCount, arguments.size()));
                            return new Block("ipc", block.toArray(new ParseResult[0]));
                        }
                    }
                }
                // endregion
            }
            // 支持一种特殊写法：
            // 在禁用无括号调用时，允许 time :: now() 这种无参数顶层函数调用，类似于静态工具
            else if (parser.check(TokenType.CONTEXT_CALL)) {
                return finishTopLevelContextCall(parser, (Identifier) name);
            }
        }
        return name;
    }

    /**
     * 获取函数最大可能的参数数量
     *
     * @param info   函数信息
     * @param exInfo 扩展函数信息集合
     * @return 最大可能的参数数量
     */
    private static int getMaxExpectedArgumentCount(Callable info, ExtensionFunctionPosition exInfo) {
        int maxCount = 0;
        // 检查普通函数
        if (info != null) {
            maxCount = Math.max(maxCount, info.getMaxParameterCount());
        }
        // 检查扩展函数
        if (exInfo != null && !exInfo.getFunctions().isEmpty()) {
            for (Map.Entry<Class<?>, Function> extFunc : exInfo.getFunctions().entrySet()) {
                maxCount = Math.max(maxCount, extFunc.getValue().getMaxParameterCount());
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
    private static boolean supportsParameterCount(Callable info, ExtensionFunctionPosition exInfo, int count) {
        // 检查普通函数
        if (info != null && info.supportsParameterCount(count)) {
            return true;
        }
        // 检查扩展函数
        if (exInfo != null && !exInfo.getFunctions().isEmpty()) {
            for (Map.Entry<Class<?>, Function> extFunc : exInfo.getFunctions().entrySet()) {
                if (extFunc.getValue().getParameterCounts().contains(count)) {
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
    private static Set<Integer> getAllParameterCounts(Callable info, ExtensionFunctionPosition exInfo) {
        Set<Integer> allCounts = new LinkedHashSet<>();
        // 添加普通函数的参数数量
        if (info != null) {
            allCounts.addAll(info.getParameterCounts());
        }
        // 添加扩展函数的参数数量
        if (exInfo != null && !exInfo.getFunctions().isEmpty()) {
            for (Map.Entry<Class<?>, Function> extFunc : exInfo.getFunctions().entrySet()) {
                allCounts.addAll(extFunc.getValue().getParameterCounts());
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

    private static ParseResult finishCall(Parser parser, Identifier callee) {
        List<ParseResult> arguments = new ArrayList<>();
        // 如果参数列表不为空
        if (!parser.check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(ExpressionParser.parse(parser));
            } while (parser.match(TokenType.COMMA));
        }
        parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
        return getFunctionCallExpression(parser, callee, arguments);
    }

    private static ParseResult finishTopLevelContextCall(Parser parser, Identifier callee) {
        return getFunctionCallExpression(parser, callee, Collections.emptyList());
    }

    private static FunctionCallExpression getFunctionCallExpression(Parser parser, Identifier callee, List<ParseResult> arguments) {
        String name = callee.getValue();
        // 获取函数
        Callable functionInfo = parser.getFunction(name);
        FunctionPosition pos = functionInfo instanceof FunctionPosition ? (FunctionPosition) functionInfo : null;
        // 只有在上下文环境中才获取扩展函数
        ExtensionFunctionPosition exPos = null;
        if (parser.getSymbolEnvironment().isContextCall()) {
            exPos = parser.getExtensionFunction(name);
        }
        // 如果函数不存在
        if (functionInfo == null && exPos == null) {
            parser.error("Function \"" + name + "\" not found");
        }
        return new FunctionCallExpression(name, arguments, pos, exPos);
    }
}
