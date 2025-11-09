package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 源码摘录
 * 用于生成类似 Rust 编译器的错误上下文展示
 */
public class SourceExcerpt {

    private final String filename;
    private final int line;
    private final int column;
    private final List<String> contextLines;
    private final int focusLineIndex;
    private final int spanStart;
    private final int spanLength;

    private SourceExcerpt(String filename,
                          int line,
                          int column,
                          List<String> contextLines,
                          int focusLineIndex,
                          int spanStart,
                          int spanLength) {
        this.filename = filename;
        this.line = line;
        this.column = column;
        this.contextLines = contextLines;
        this.focusLineIndex = focusLineIndex;
        this.spanStart = spanStart;
        this.spanLength = spanLength;
    }

    /**
     * 从编译上下文和 Token 创建源码摘录
     *
     * @param context 编译上下文
     * @param token   相关的词法单元
     * @return 源码摘录，如果 token 无位置信息则返回 null
     */
    public static SourceExcerpt from(CompilationContext context, Token token) {
        return from(context, token, 1);
    }

    /**
     * 从编译上下文和 Token 创建源码摘录
     *
     * @param context     编译上下文
     * @param token       相关的词法单元
     * @param linesAround 前后显示的行数
     * @return 源码摘录，如果 token 无位置信息则返回 null
     */
    public static SourceExcerpt from(CompilationContext context, Token token, int linesAround) {
        if (token == null || token.getLine() <= 0) {
            return null;
        }

        String source = context.getSource();
        if (source == null || source.isEmpty()) {
            return null;
        }

        String[] lines = source.split("\n", -1);
        int line = token.getLine();
        int column = Math.max(1, token.getColumn());

        // 确保行号有效
        if (line > lines.length) {
            return null;
        }

        // 计算上下文范围
        int startLine = Math.max(1, line - linesAround);
        int endLine = Math.min(lines.length, line + linesAround);

        // 提取上下文行
        List<String> contextLines = new ArrayList<>(Arrays.asList(lines).subList(startLine - 1, endLine));

        // 计算焦点行在上下文中的索引
        int focusLineIndex = line - startLine;
        String focusLine = contextLines.get(focusLineIndex);
        int focusLineLength = focusLine.length();
        int zeroBasedColumn;
        if (focusLineLength == 0) {
            zeroBasedColumn = 0;
        } else {
            zeroBasedColumn = Math.min(Math.max(column - 1, 0), focusLineLength - 1);
        }

        // 计算 span 长度（token 的长度）
        int spanLength = token.getLexeme() != null ? token.getLexeme().length() : 1;
        if (spanLength <= 0) {
            spanLength = 1;
        }
        if (focusLineLength > 0) {
            spanLength = Math.min(spanLength, Math.max(focusLineLength - zeroBasedColumn, 1));
        } else {
            spanLength = 1;
        }
        return new SourceExcerpt(context.getSource(), line, column, contextLines, focusLineIndex, zeroBasedColumn, spanLength);
    }

    /**
     * 渲染成 Rust 风格的格式化字符串
     *
     * @return 格式化后的上下文摘录
     */
    public String render() {
        if (contextLines.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 计算行号的最大宽度
        int maxLineNum = line - focusLineIndex + contextLines.size() - 1;
        int lineNumWidth = String.valueOf(maxLineNum).length();

        // 渲染每一行
        for (int i = 0; i < contextLines.size(); i++) {
            int currentLine = line - focusLineIndex + i;
            String lineContent = contextLines.get(i);

            // 展开制表符
            String expandedLine = expandTabs(lineContent);

            // 行号前缀
            sb.append(String.format(" %" + lineNumWidth + "d | ", currentLine));
            sb.append(expandedLine);
            sb.append("\n");

            // 如果是焦点行，添加指示符
            if (i == focusLineIndex) {
                sb.append(" ");
                for (int j = 0; j < lineNumWidth; j++) {
                    sb.append(" ");
                }
                sb.append(" | ");

                // 计算展开后的列位置（列号从1开始，需要转换为0-based）
                int expandedColumn = calculateExpandedColumn(lineContent, spanStart);

                // 添加前导空格
                for (int j = 0; j < expandedColumn; j++) {
                    sb.append(" ");
                }

                // 添加指示符
                sb.append("^");
                for (int j = 1; j < spanLength; j++) {
                    sb.append("~");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 展开制表符为空格（假设 tab = 4 spaces）
     */
    private String expandTabs(String line) {
        return line.replace("\t", "    ");
    }

    /**
     * 计算展开制表符后的列位置
     *
     * @param line   原始行内容
     * @param column 列位置（0-based）
     * @return 展开后的列位置
     */
    private int calculateExpandedColumn(String line, int column) {
        int expanded = 0;
        int safeColumn = Math.max(0, Math.min(column, line.length()));
        for (int i = 0; i < safeColumn; i++) {
            if (line.charAt(i) == '\t') {
                expanded += 4;
            } else {
                expanded++;
            }
        }
        return expanded;
    }

    /**
     * 格式化完整的诊断信息（包含位置头）
     *
     * @param errorCode 错误代码（可选）
     * @param message   错误消息
     * @return 完整的格式化诊断
     */
    public String formatDiagnostic(String errorCode, String message) {
        StringBuilder sb = new StringBuilder();
        // 错误头
        if (errorCode != null && !errorCode.isEmpty()) {
            sb.append("error[").append(errorCode).append("]: ");
        } else {
            sb.append("error: ");
        }
        sb.append(message).append("\n");
        // 位置信息
        sb.append("  --> ").append(filename).append(":").append(line).append(":").append(column).append("\n");
        // 上下文摘录
        String rendered = render();
        if (!rendered.isEmpty()) {
            sb.append(rendered);
        }
        return sb.toString();
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
