package org.tabooproject.fluxon.parser.util;

/**
 * 伪代码生成工具类
 */
public class StringUtils {
    
    /**
     * 生成指定数量的缩进空格
     *
     * @param indent 缩进级别
     * @return 缩进字符串
     */
    public static String getIndent(int indent) {
        return "    ".repeat(Math.max(0, indent));
    }
    
    /**
     * 为多行文本的每一行添加缩进
     *
     * @param text 多行文本
     * @param indent 缩进级别
     * @return 添加缩进后的文本
     */
    public static String indentMultiline(String text, int indent) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        String indentStr = getIndent(indent);
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(indentStr).append(lines[i]);
        }
        
        return sb.toString();
    }
}