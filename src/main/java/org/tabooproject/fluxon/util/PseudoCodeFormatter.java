package org.tabooproject.fluxon.util;

/**
 * 伪代码格式化器
 * 将单行伪代码转换为格式化的多行代码
 */
public class PseudoCodeFormatter {
    // 缩进字符，默认为4个空格
    private static final String INDENT = "    ";
    
    /**
     * 格式化伪代码
     * 
     * @param pseudoCode 单行伪代码
     * @return 格式化后的多行伪代码
     */
    public static String format(String pseudoCode) {
        if (pseudoCode == null || pseudoCode.trim().isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        boolean skipWhitespace = false; // 是否跳过空白字符
        boolean inString = false; // 是否在字符串内
        char stringQuote = 0; // 字符串的引号类型 (' 或 ")
        
        for (int i = 0; i < pseudoCode.length(); i++) {
            char c = pseudoCode.charAt(i);
            
            // 处理字符串
            if ((c == '"' || c == '\'') && (i == 0 || pseudoCode.charAt(i - 1) != '\\')) {
                if (!inString) {
                    inString = true;
                    stringQuote = c;
                } else if (c == stringQuote) {
                    inString = false;
                }
                result.append(c);
                continue;
            }
            
            // 如果在字符串内，直接添加字符
            if (inString) {
                result.append(c);
                continue;
            }
            
            // 跳过空白字符
            if (skipWhitespace && Character.isWhitespace(c)) {
                continue;
            }
            skipWhitespace = false;
            
            // 处理左花括号：增加缩进级别
            if (c == '{') {
                result.append("{\n");
                indentLevel++;
                addIndent(result, indentLevel);
                skipWhitespace = true;
                continue;
            }
            
            // 处理右花括号：减少缩进级别
            if (c == '}') {
                // 在右花括号前换行并缩进
                result.append('\n');
                indentLevel = Math.max(0, indentLevel - 1);
                addIndent(result, indentLevel);
                result.append('}');
                
                // 如果右花括号后面不是分号或右花括号，则换行
                if (i + 1 < pseudoCode.length() && pseudoCode.charAt(i + 1) != ';' && pseudoCode.charAt(i + 1) != '}') {
                    result.append('\n');
                    addIndent(result, indentLevel);
                    skipWhitespace = true;
                }
                continue;
            }
            
            // 处理分号：在分号后换行
            if (c == ';') {
                if (i + 1 < pseudoCode.length() && pseudoCode.charAt(i + 1) != '}') {
                    result.append('\n');
                    addIndent(result, indentLevel);
                    skipWhitespace = true;
                }
                continue;
            }
            
            // 添加其他字符
            result.append(c);
        }
        
        return result.toString();
    }
    
    /**
     * 添加缩进
     * 
     * @param sb 字符串构建器
     * @param level 缩进级别
     */
    private static void addIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
    }
}