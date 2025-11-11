package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.Parser;

import java.util.LinkedHashMap;

/**
 * 参数解析器
 * 提供通用的参数列表解析逻辑，支持多种语法风格
 */
public class ParameterParser {

    /**
     * 解析参数列表
     * 支持三种语法：
     * 1. 带括号：(x, y, z)
     * 2. 无括号多参数：x y z 或 x, y, z
     * 3. 单参数简写：x
     *
     * @param parser                       解析器
     * @param allowUnparenthesizedMultiple 是否允许无括号的多参数
     * @param stopTokens                   停止解析的 token 类型
     * @return 参数名到索引的映射
     */
    public static LinkedHashMap<String, Integer> parseParameters(Parser parser, boolean allowUnparenthesizedMultiple, TokenType... stopTokens) {
        LinkedHashMap<String, Integer> parameters = new LinkedHashMap<>();
        // 带括号的参数列表: (x, y, z)
        if (parser.match(TokenType.LEFT_PAREN)) {
            if (!parser.check(TokenType.RIGHT_PAREN)) {
                do {
                    addParameter(parser, parameters);
                } while (parser.match(TokenType.COMMA) && !parser.check(TokenType.RIGHT_PAREN));
            }
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");
            return parameters;
        }
        // 无括号参数：需要检查是否允许
        if (!allowUnparenthesizedMultiple) {
            return parameters;
        }
        // 无括号的参数列表: x, y, z 或 x y z
        while (parser.check(TokenType.IDENTIFIER)) {
            addParameter(parser, parameters);
            // 检查是否遇到停止符
            if (isStopToken(parser.peek().getType(), stopTokens)) {
                break;
            }
            // 可选的逗号分隔
            if (!parser.match(TokenType.COMMA)) {
                // 如果没有逗号，检查下一个是否还是标识符（空格分隔）
                if (!parser.check(TokenType.IDENTIFIER)) {
                    break;
                }
            }
        }
        return parameters;
    }
    
    /**
     * 添加单个参数
     */
    private static void addParameter(Parser parser, LinkedHashMap<String, Integer> parameters) {
        Token paramToken = parser.consume(TokenType.IDENTIFIER, "Expected parameter name");
        String paramName = paramToken.getLexeme();
        // 检查参数重复
        if (parameters.containsKey(paramName)) {
            parser.error("Duplicate parameter name: " + paramName);
        }
        // 注册参数为局部变量
        parser.defineVariable(paramName);
        int index = parser.getSymbolEnvironment().getLocalVariable(paramName);
        parameters.put(paramName, index);
    }
    
    /**
     * 检查当前 token 是否是停止符
     */
    private static boolean isStopToken(TokenType token, TokenType... stopTokens) {
        if (stopTokens == null) {
            return false;
        }
        for (TokenType stopToken : stopTokens) {
            if (token == stopToken) {
                return true;
            }
        }
        return false;
    }
}
