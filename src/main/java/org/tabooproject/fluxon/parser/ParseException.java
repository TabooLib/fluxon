package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.lexer.Token;

import java.util.List;

/**
 * 解析异常
 * 表示解析过程中发生的错误
 */
public class ParseException extends RuntimeException {

    private final Token token;
    private final List<ParseResult> results;

    /**
     * 创建解析异常
     *
     * @param message 错误消息
     * @param token 相关的词法单元
     */
    public ParseException(String message, Token token, List<ParseResult> results) {
        super(String.format("%s at line %d, column %d: %s", message, token.getLine(), token.getColumn(), token.getStringValue()));
        this.token = token;
        this.results = results;
    }
    
    /**
     * 获取相关的词法单元
     *
     * @return 相关的词法单元
     */
    public Token getToken() {
        return token;
    }

    /**
     * 获取已经解析出的结果
     *
     * @return 已经解析出的结果
     */
    public List<ParseResult> getResults() {
        return results;
    }
}