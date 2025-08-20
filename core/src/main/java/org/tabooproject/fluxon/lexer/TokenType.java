package org.tabooproject.fluxon.lexer;

/**
 * 词法单元类型
 * 定义了 Fluxon 语言中的所有词法单元类型
 */
public enum TokenType {
    // 关键字
    DEF("def"),
    FUN("fun"),
    VAL("val"),
    VAR("var"),
    IF("if"),
    THEN("then"),
    ELSE("else"),
    FOR("for"),
    WHEN("when"),
    IS("is"),
    IN("in"),
    ASYNC("async"),
    AWAIT("await"),
    RETURN("return"),
    TRY("try"),
    CATCH("catch"),
    FINALLY("finally"),
    WHILE("while"),
    BREAK("break"),
    CONTINUE("continue"),

    // 标识符和字面量
    IDENTIFIER,    // 标识符
    STRING,        // 字符串字面量
    INTEGER,       // 整数字面量
    FLOAT,         // 浮点数字面量
    LONG,          // 长整型字面量
    DOUBLE,        // 双精度浮点数字面量
    TRUE,          // 布尔字面量
    FALSE,         // 布尔字面量
    NULL,          // 空值

    // 操作符
    PLUS("+"),             // 加法
    MINUS("-"),            // 减法
    MULTIPLY("*"),         // 乘法
    DIVIDE("/"),           // 除法
    MODULO("%"),           // 取模

    EQUAL("=="),           // 等于
    NOT_EQUAL("!="),       // 不等于
    GREATER(">"),          // 大于
    LESS("<"),             // 小于
    GREATER_EQUAL(">="),   // 大于等于
    LESS_EQUAL("<="),      // 小于等于

    ASSIGN("="),           // 赋值
    PLUS_ASSIGN("+="),     // 加法赋值
    MINUS_ASSIGN("-="),    // 减法赋值
    MULTIPLY_ASSIGN("*="), // 乘法赋值
    DIVIDE_ASSIGN("/="),   // 除法赋值
    MODULO_ASSIGN("%="),   // 取模赋值

    AND("&&"),             // 逻辑与
    OR("||"),              // 逻辑或
    NOT("!"),              // 逻辑非

    // 特殊符号
    DOT("."),                  // 点
    COMMA(","),                // 逗号
    COLON(":"),                // 冒号
    SEMICOLON(";"),            // 分号
    ARROW("->"),               // 箭头
    RANGE(".."),               // 范围
    RANGE_EXCLUSIVE("..<"),    // 排除上界的范围

    LEFT_PAREN("("),           // 左括号
    RIGHT_PAREN(")"),          // 右括号
    LEFT_BRACE("{"),           // 左花括号
    RIGHT_BRACE("}"),          // 右花括号
    LEFT_BRACKET("["),         // 左方括号
    RIGHT_BRACKET("]"),        // 右方括号

    QUESTION("?"),             // 问号
    QUESTION_COLON("?:"),      // Elvis 操作符
    AMPERSAND("&"),            // 变量引用前缀
    CONTEXT_CALL("::"),        // 上下文调用操作符
    AT("@"),                   // 注解符号

    // 其他
    EOF,        // 文件结束
    UNKNOWN;    // 未知类型

    private final String text;

    TokenType() {
        this.text = this.name().toLowerCase();
    }

    TokenType(String text) {
        this.text = text;
    }

    /**
     * 获取词法单元的文本表示
     *
     * @return 文本表示
     */
    public String getText() {
        return text;
    }

    /**
     * 是否为表达式结束标记
     */
    public boolean isEndOfExpression() {
        switch (this) {
            case EOF:
            case SEMICOLON:
            case RIGHT_PAREN:
            case RIGHT_BRACE:
            case RIGHT_BRACKET:
                return true;
            default:
                return false;
        }
    }

    /**
     * 是否为操作符
     */
    public boolean isOperator() {
        switch (this) {
            case PLUS:
            case MINUS:
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
            case EQUAL:
            case NOT_EQUAL:
            case LESS:
            case LESS_EQUAL:
            case GREATER:
            case GREATER_EQUAL:
            case AND:
            case OR:
            case ASSIGN:
            case CONTEXT_CALL:
                return true;
            default:
                return false;
        }
    }
}