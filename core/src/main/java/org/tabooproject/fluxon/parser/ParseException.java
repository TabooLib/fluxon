package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.lexer.Token;

import java.util.List;

/**
 * 解析异常
 * 表示解析过程中发生的错误
 */
public class ParseException extends RuntimeException {

    private final String reason;
    private final Token token;
    private final List<ParseResult> results;
    private final SourceExcerpt excerpt;

    /**
     * 创建解析异常（带源码摘录）
     *
     * @param reason 错误原因
     * @param token 相关的词法单元
     * @param results 已解析的结果
     * @param excerpt 源码摘录
     */
    public ParseException(String reason, Token token, List<ParseResult> results, SourceExcerpt excerpt) {
        super(String.format("%s at line: %d, column: %d, next: %s", reason, token.getLine(), token.getColumn(), token.getLexeme()));
        this.reason = reason;
        this.token = token;
        this.results = results;
        this.excerpt = excerpt;
    }

    /**
     * 创建解析异常（简化版，用于子类，带源码摘录）
     *
     * @param message 错误消息
     * @param token 相关的词法单元
     * @param excerpt 源码摘录
     */
    protected ParseException(String message, Token token, SourceExcerpt excerpt) {
        super(message);
        this.reason = message;
        this.token = token;
        this.results = null;
        this.excerpt = excerpt;
    }

    /**
     * 获取错误原因
     *
     * @return 错误原因
     */
    public String getReason() {
        return reason;
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
    
    /**
     * 获取源码摘录
     *
     * @return 源码摘录，可能为 null
     */
    public SourceExcerpt getExcerpt() {
        return excerpt;
    }
    
    /**
     * 格式化输出带源码上下文的错误信息
     *
     * @return 格式化后的错误信息
     */
    public String formatWithSource() {
        if (excerpt == null) {
            return getMessage();
        }
        return excerpt.formatDiagnostic(null, reason);
    }
}