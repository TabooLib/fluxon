package org.tabooproject.fluxon.lexer;

/**
 * 词法单元
 * 表示源代码中的最小语法单位
 */
public class Token {
    private final TokenType type;
    private final Object value;
    private final int line;
    private final int column;
    private final int endLine;
    private final int endColumn;

    /**
     * 创建词法单元
     *
     * @param type 词法单元类型
     */
    public Token(TokenType type) {
        this(type, type.getText());
    }

    /**
     * 创建词法单元
     *
     * @param type 词法单元类型
     * @param value 词法单元值
     */
    public Token(TokenType type, Object value) {
        this(type, value, -1, -1);
    }
    
    /**
     * 创建词法单元
     * 
     * @param type 词法单元类型
     * @param value 词法单元值
     * @param line 行号
     * @param column 列号
     */
    public Token(TokenType type, Object value, int line, int column) {
        this(type, value, line, column, line, column + (value != null ? value.toString().length() : 0));
    }
    
    /**
     * 创建词法单元（完整位置信息）
     * 
     * @param type 词法单元类型
     * @param value 词法单元值
     * @param line 行号
     * @param column 列号
     * @param endLine 结束行号
     * @param endColumn 结束列号
     */
    public Token(TokenType type, Object value, int line, int column, int endLine, int endColumn) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }
    
    /**
     * 获取词法单元类型
     * 
     * @return 词法单元类型
     */
    public TokenType getType() {
        return type;
    }

    /**
     * 获取词法单元值
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * 获取词法单元值的字符串形式
     */
    public String getLexeme() {
        return value.toString();
    }
    
    /**
     * 获取行号
     */
    public int getLine() {
        return line;
    }
    
    /**
     * 获取列号
     */
    public int getColumn() {
        return column;
    }
    
    /**
     * 获取结束行号
     */
    public int getEndLine() {
        return endLine;
    }
    
    /**
     * 获取结束列号
     */
    public int getEndColumn() {
        return endColumn;
    }
    
    /**
     * 检查词法单元类型是否为指定类型
     * 
     * @param type 要检查的类型
     * @return 是否为指定类型
     */
    public boolean is(TokenType type) {
        return this.type == type;
    }
    
    /**
     * 检查词法单元类型是否为指定类型之一
     * 
     * @param types 要检查的类型数组
     * @return 是否为指定类型之一
     */
    public boolean isOneOf(TokenType... types) {
        for (TokenType t : types) {
            if (this.type == t) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        if (endLine == line) {
            return String.format("%s('%s') at %d:%d-%d", type, value, line, column, endColumn);
        } else {
            return String.format("%s('%s') at %d:%d-%d:%d", type, value, line, column, endLine, endColumn);
        }
    }
}