package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SymbolEnvironment;
import org.tabooproject.fluxon.parser.expression.LambdaExpression;

import java.util.Collections;
import java.util.LinkedHashMap;
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
        
        // 解析参数列表
        LinkedHashMap<String, Integer> parameters = new LinkedHashMap<>();
        // 生成唯一的 lambda 函数名用于符号表
        String lambdaName = "lambda@" + lambdaToken.getLine() + ":" + lambdaToken.getColumn();
        // 保存当前作用域并切换到 lambda 作用域
        SymbolEnvironment.FunctionScopeSnapshot snapshot = parser.getSymbolEnvironment().pushFunctionScope(lambdaName);
        try {
            // 解析参数
            if (parser.match(TokenType.LEFT_PAREN)) {
                // 有括号的参数列表: lambda (x, y) -> ...
                if (!parser.check(TokenType.RIGHT_PAREN)) {
                    do {
                        Token paramToken = parser.consume(TokenType.IDENTIFIER, "Expected parameter name");
                        String paramName = paramToken.getLexeme();
                        // 检查参数重复
                        if (parameters.containsKey(paramName)) {
                            parser.error("Duplicate parameter name: " + paramName);
                            return null;
                        }
                        // 注册参数为局部变量
                        parser.defineVariable(paramName);
                        int index = parser.getSymbolEnvironment().getLocalVariable(paramName);
                        parameters.put(paramName, index);
                        
                    } while (parser.match(TokenType.COMMA));
                }
                parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after lambda parameters");
            }
            // 否则无参数: lambda -> ... 或 lambda { ... }
            // 解析函数体
            ParseResult body;
            if (parser.match(TokenType.ARROW)) {
                // 箭头语法: lambda (x) -> expression
                body = ExpressionParser.parse(parser);
            } else if (parser.match(TokenType.LEFT_BRACE)) {
                // 代码块语法: lambda (x) { statements }
                // BlockParser.parse 期望 { 已被消费
                body = BlockParser.parse(parser);
            } else {
                parser.error("Expected '->' or '{' after lambda parameters");
                return null;
            }
            // 获取局部变量集合
            Set<String> localVariables = parser.getSymbolEnvironment().getLocalVariables().getOrDefault(lambdaName, Collections.emptySet());
            return new LambdaExpression(parameters, body, localVariables, isAsync, isPrimarySync);
        } finally {
            // 恢复外层作用域
            parser.getSymbolEnvironment().popFunctionScope(snapshot);
        }
    }
}
