package org.tabooproject.fluxon.parser;

import java.util.List;

/**
 * 多重解析异常
 * 当解析过程中遇到多个错误时使用
 */
public class MultipleParseException extends ParseException {

    private final List<ParseException> exceptions;

    public MultipleParseException(List<ParseException> exceptions) {
        super(buildMessage(exceptions), exceptions.get(0).getToken(), exceptions.get(0).getExcerpt());
        this.exceptions = exceptions;
    }

    /**
     * 格式化输出所有错误的详细诊断信息（包含源码上下文）
     *
     * @return 格式化后的诊断信息
     */
    @Override
    public String formatDiagnostic() {
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(exceptions.size()).append(" parse error(s):\n\n");
        for (int i = 0; i < exceptions.size(); i++) {
            ParseException ex = exceptions.get(i);
            // 如果有源码摘录，使用详细格式
            if (ex.getExcerpt() != null) {
                sb.append(ex.formatDiagnostic());
            } else {
                // 否则使用简单格式
                sb.append("error: ").append(ex.getReason()).append("\n");
                sb.append("  at line ").append(ex.getToken().getLine()).append(", column ").append(ex.getToken().getColumn()).append("\n");
            }
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

    /**
     * 格式化错误消息，包含每个异常的简单描述
     *
     * @param exceptions 解析异常列表
     * @return 格式化后的错误消息
     */
    public static String buildMessage(List<ParseException> exceptions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(exceptions.size()).append(" parse error(s):\n");
        for (int i = 0; i < exceptions.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(exceptions.get(i));
            if (i < exceptions.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
