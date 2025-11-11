package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.LambdaExpression;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lambda 表达式解析器
 * 负责解析 lambda 表达式：lambda (x, y) -> expr 或 lambda (x, y) { ... }
 */
public class LambdaParser {

    /**
     * 解析 lambda 表达式
     * 语法:
     * - lambda (param1, param2) -> expression
     * - lambda (param1, param2) { statements }
     * - lambda -> expression  (无参数)
     * - lambda { statements }  (无参数)
     *
     * @param parser 解析器
     * @return Lambda 表达式
     */
    public static ParseResult parse(Parser parser) {
        Token lambdaToken = parser.previous(); // 已消费 lambda 关键字
        boolean isAsync = false;
        boolean isPrimarySync = false;

        // 检查 async/sync 修饰符（虽然 lambda 通常不需要，但保留扩展性）
        // 注意：这里假设 async/sync 在 lambda 之前，如 async lambda (x) -> x

        // 生成唯一的 lambda 函数名用于符号表
        String lambdaName = "lambda@" + lambdaToken.getLine() + ":" + lambdaToken.getColumn();
        // 保存当前作用域并切换到 lambda 作用域
        FunctionScopeSnapshot snapshot = parser.getSymbolEnvironment().pushFunctionScope(lambdaName);
        try {
            // 解析参数列表（支持单参数简写和逗号分隔的多参数）
            LinkedHashMap<String, Integer> parameters = ParameterParser.parseParameters(
                    parser,
                    true,                                  // lambda 支持无括号参数（单个或逗号分隔）
                    TokenType.ARROW, TokenType.LEFT_BRACE  // 遇到箭头或左大括号停止
            );
            // 消费可选的箭头
            parser.match(TokenType.ARROW);
            // 解析函数体
            ParseResult body;
            // 如果有左大括号，则解析为 Block 函数体
            if (parser.match(TokenType.LEFT_BRACE)) {
                body = BlockParser.parse(parser);
            } else {
                body = ExpressionParser.parse(parser);
            }
            // 获取局部变量集合
            LinkedHashMap<String, Integer> localTable = parser.getSymbolEnvironment().getLocalVariableTable(lambdaName);
            Set<String> localVariables = localTable == null ? Collections.emptySet() : new LinkedHashSet<>(localTable.keySet());
            List<CapturedVariable> capturedVariables = parser.getSymbolEnvironment().drainCapturedVariables(lambdaName);
            return new LambdaExpression(parameters, body, localVariables, capturedVariables, isAsync, isPrimarySync);
        } finally {
            // 恢复外层作用域
            parser.getSymbolEnvironment().popFunctionScope(snapshot);
        }
    }
}
