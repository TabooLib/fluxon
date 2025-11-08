package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.lexer.Token;

import java.util.List;

/**
 * 多重解析异常
 * 当解析过程中遇到多个错误时使用
 */
public class MultipleParseException extends ParseException {

    private final List<ParseException> exceptions;

    public MultipleParseException(List<ParseException> exceptions) {
        super(buildMessage(exceptions), exceptions.get(0).getToken());
        this.exceptions = exceptions;
    }

    private static String buildMessage(List<ParseException> exceptions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(exceptions.size()).append(" parse error(s):\n");
        for (int i = 0; i < exceptions.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(exceptions.get(i).getMessage());
            if (i < exceptions.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 获取所有异常
     */
    public List<ParseException> getExceptions() {
        return exceptions;
    }

    /**
     * 获取错误数量
     */
    public int getErrorCount() {
        return exceptions.size();
    }
}
