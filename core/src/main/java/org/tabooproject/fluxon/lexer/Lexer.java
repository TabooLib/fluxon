package org.tabooproject.fluxon.lexer;

import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompilationPhase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 词法分析器
 * 将源代码转换为词法单元序列
 */
public class Lexer implements CompilationPhase<List<Token>> {

    private char[] sourceChars;
    private int sourceLength;
    private int position = 0;
    private int line = 1;
    private int column = 1;

    private char curr;        // 当前字符缓存
    private char next;        // 下一个字符缓存

    // 关键字映射表
    private static final Map<String, TokenType> KEYWORDS;

    // 单字符映射表
    private static final Map<Character, TokenType> SINGLE_CHAR_TOKENS;

    // 预定义一些常用的字符串值，减少字符串创建
    private static final String EMPTY_STRING = "";
    private static final String AND_STRING = "&&";
    private static final String OR_STRING = "||";
    private static final String ARROW_STRING = "->";

    static {
        KEYWORDS = new HashMap<>(32); // 预分配容量
        // 初始化关键字映射
        KEYWORDS.put("def", TokenType.DEF);
        KEYWORDS.put("fun", TokenType.FUN);
        KEYWORDS.put("val", TokenType.VAL);
        KEYWORDS.put("var", TokenType.VAR);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("then", TokenType.THEN);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("when", TokenType.WHEN);
        KEYWORDS.put("is", TokenType.IS);
        KEYWORDS.put("in", TokenType.IN);
        KEYWORDS.put("async", TokenType.ASYNC);
        KEYWORDS.put("await", TokenType.AWAIT);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("try", TokenType.TRY);
        KEYWORDS.put("catch", TokenType.CATCH);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("null", TokenType.NULL);

        // 初始化单字符映射
        SINGLE_CHAR_TOKENS = new HashMap<>(32); // 预分配容量
        SINGLE_CHAR_TOKENS.put('+', TokenType.PLUS);
        SINGLE_CHAR_TOKENS.put('-', TokenType.MINUS);
        SINGLE_CHAR_TOKENS.put('*', TokenType.MULTIPLY);
        SINGLE_CHAR_TOKENS.put('/', TokenType.DIVIDE);
        SINGLE_CHAR_TOKENS.put('%', TokenType.MODULO);
        SINGLE_CHAR_TOKENS.put('=', TokenType.ASSIGN);
        SINGLE_CHAR_TOKENS.put('>', TokenType.GREATER);
        SINGLE_CHAR_TOKENS.put('<', TokenType.LESS);
        SINGLE_CHAR_TOKENS.put('!', TokenType.NOT);
        SINGLE_CHAR_TOKENS.put('.', TokenType.DOT);
        SINGLE_CHAR_TOKENS.put(',', TokenType.COMMA);
        SINGLE_CHAR_TOKENS.put(':', TokenType.COLON);
        SINGLE_CHAR_TOKENS.put(';', TokenType.SEMICOLON);
        SINGLE_CHAR_TOKENS.put('(', TokenType.LEFT_PAREN);
        SINGLE_CHAR_TOKENS.put(')', TokenType.RIGHT_PAREN);
        SINGLE_CHAR_TOKENS.put('{', TokenType.LEFT_BRACE);
        SINGLE_CHAR_TOKENS.put('}', TokenType.RIGHT_BRACE);
        SINGLE_CHAR_TOKENS.put('[', TokenType.LEFT_BRACKET);
        SINGLE_CHAR_TOKENS.put(']', TokenType.RIGHT_BRACKET);
        SINGLE_CHAR_TOKENS.put('?', TokenType.QUESTION);
        SINGLE_CHAR_TOKENS.put('|', TokenType.UNKNOWN);
    }

    /**
     * 执行词法分析
     *
     * @param context 编译上下文
     * @return 词法单元列表
     */
    @Override
    public List<Token> process(CompilationContext context) {
        this.sourceChars = context.getSource().toCharArray();
        this.sourceLength = sourceChars.length;

        // 预加载第一个字符，避免重复边界检查
        if (sourceLength > 0) {
            curr = sourceChars[0];
            next = sourceLength > 1 ? sourceChars[1] : '\0';
        } else {
            curr = '\0';
            next = '\0';
        }
        return tokenize();
    }

    /**
     * 将源代码转换为词法单元序列
     *
     * @return 词法单元列表
     */
    private List<Token> tokenize() {
        // 预分配合适大小的列表，避免频繁扩容
        List<Token> tokens = new ArrayList<>(Math.max(512, sourceLength / 8));

        // 重置状态
        position = 0;
        line = 1;
        column = 1;

        // 预加载第一个字符
        if (sourceLength > 0) {
            curr = sourceChars[0];
            next = sourceLength > 1 ? sourceChars[1] : '\0';
        } else {
            curr = '\0';
            next = '\0';
        }

        while (position < sourceLength) {
            // 使用缓存的字符，避免调用peek()
            char c = this.curr;

            // 使用内联的方式处理常见情况，减少方法调用开销
            if (c <= ' ') { // 快速空白字符检查
                consumeWhitespace();
            }
            // 处理双字符逻辑操作符
            else if (c == '&' && next == '&') {
                // 处理 && 操作符
                int startLine = line;
                int startColumn = column;
                advance(); // 消费第一个 &
                advance(); // 消费第二个 &
                tokens.add(new Token(TokenType.AND, AND_STRING, startLine, startColumn));
            } else if (c == '|' && next == '|') {
                // 处理 || 操作符
                int startLine = line;
                int startColumn = column;
                advance(); // 消费第一个 |
                advance(); // 消费第二个 |
                tokens.add(new Token(TokenType.OR, OR_STRING, startLine, startColumn));
            } else if (c == '-' && next == '>') {
                // 处理 -> 操作符
                int startLine = line;
                int startColumn = column;
                advance(); // 消费 -
                advance(); // 消费 >
                tokens.add(new Token(TokenType.ARROW, ARROW_STRING, startLine, startColumn));
            } else if (c == '/' && next == '/') {
                // 行注释 - 快速消费
                consumeLineComment();
            } else if (c == '/' && next == '*') {
                // 块注释 - 快速消费
                consumeBlockComment();
            } else if (c == '"' || c == '\'') {
                tokens.add(consumeString());
            }
            // 数字检查使用优化的范围检查而不是Character.isDigit()
            else if (c >= '0' && c <= '9') {
                tokens.add(consumeNumber());
            }
            // 标识符检查，扩展为支持中文字符
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || Character.isIdeographic(c) || isChineseChar(c)) {
                tokens.add(consumeIdentifier());
            }
            // 处理单个 & 字符 
            else if (c == '&') {
                tokens.add(new Token(TokenType.AMPERSAND, "&", line, column));
                advance();
            } else {
                tokens.add(consumeOperator());
            }
        }

        // 添加 EOF 标记
        tokens.add(new Token(TokenType.EOF, EMPTY_STRING, line, column));
        return tokens;
    }

    /**
     * 消费空白字符
     */
    private void consumeWhitespace() {
        // 直接使用 currentChar 而不是调用 peek()
        while (position < sourceLength && curr <= ' ') {
            char c = this.curr;
            if (c == '\n') {
                line++;
                column = 1;
            }
            advance();
        }
    }

    /**
     * 消费行注释
     */
    private void consumeLineComment() {
        // 跳过 //
        advance();
        // 消费直到行尾
        do {
            advance();
        } while (position < sourceLength && curr != '\n');
    }

    /**
     * 消费块注释
     */
    private void consumeBlockComment() {
        // 跳过 /*
        advance(); // 跳过 /
        advance(); // 跳过 *
        // 消费直到 */
        while (position < sourceLength) {
            if (curr == '*' && next == '/') {
                advance(); // 跳过 *
                advance(); // 跳过 /
                break;
            }
            if (curr == '\n') {
                line++;
                column = 1;
            }
            advance();
        }
    }

    /**
     * 检查是否标识符的值是关键字
     *
     * @param identifier 标识符文本
     * @return 对应的 TokenType
     */
    private TokenType getIdentifierType(String identifier) {
        return KEYWORDS.getOrDefault(identifier, TokenType.IDENTIFIER);
    }

    /**
     * 消费字符串字面量
     *
     * @return 字符串词法单元
     */
    private Token consumeString() {
        int startLine = line;
        int startColumn = column;

        // 直接使用 currentChar 而不是 advance() 后的返回值
        char quote = curr;
        // 消费引号
        advance();

        // 使用 StringBuilder，避免中间字符串创建
        StringBuilder sb = new StringBuilder(Math.min(32, sourceLength - position));
        while (position < sourceLength && curr != quote) {
            char c = this.curr;
            if (c == '\\') {
                // 消费反斜杠
                advance();
                // 检查反斜杠后的字符是否合法
                if (position >= sourceLength) {
                    break;
                }
                char escaped = this.curr;
                advance();
                switch (escaped) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '\'':
                        sb.append('\'');
                        break;
                    case '"':
                        sb.append('"');
                        break;
                    default:
                        sb.append(escaped);
                }
            } else {
                sb.append(c);
                advance();
            }
        }
        // 消费结束引号，如果存在
        if (position < sourceLength) {
            advance();
        }
        // 从缓冲区创建字符串
        return new Token(TokenType.STRING, sb.toString(), startLine, startColumn);
    }

    /**
     * 消费数字字面量
     *
     * @return 数字词法单元
     */
    private Token consumeNumber() {
        int startLine = line;
        int startColumn = column;
        int start = position;
        boolean isDouble = false;

        // 消费整数部分 - 使用字符范围检查代替 Character.isDigit()
        while (position < sourceLength && curr >= '0' && curr <= '9') {
            advance();
        }

        // 检查是否有小数点
        if (position < sourceLength && curr == '.') {
            // 检查下一个字符是否也是小数点（范围操作符的开始）
            if (next == '.') {
                // 这是范围操作符的开始，不是浮点数的一部分
                return new Token(TokenType.INTEGER, parseInteger(start, position), startLine, startColumn);
            } else {
                isDouble = true;
                // 消费小数点
                // 消费小数部分 - 使用字符范围检查
                do advance();
                while (position < sourceLength && curr >= '0' && curr <= '9');
            }
        }

        // 提前处理简单情况，减少后续不必要的检查
        if (!isDouble && position < sourceLength && curr != 'e' && curr != 'E') {
            // 检查是否为LONG类型（带L或l后缀）
            if (curr == 'L' || curr == 'l') {
                advance(); // 消费 L 或 l
                return new Token(TokenType.LONG, parseLong(start, position - 1), startLine, startColumn);
            }
            // 优化纯整数情况，直接提取子字符串并返回
            return new Token(TokenType.INTEGER, parseInteger(start, position), startLine, startColumn);
        }

        // 检查是否有指数部分
        if (position < sourceLength && (curr == 'e' || curr == 'E')) {
            isDouble = true;
            advance(); // 消费 e 或 E
            // 检查指数符号
            if (position < sourceLength && (curr == '+' || curr == '-')) {
                advance(); // 消费 + 或 -
            }
            // 消费指数部分
            while (position < sourceLength && curr >= '0' && curr <= '9') {
                advance(); // 消费数字
            }
        }

        // 记录最终取值的结束位置
        if (position < sourceLength) {
            char suffix = curr;
            // 长整型
            switch (suffix) {
                case 'l':
                case 'L':
                    advance();
                    return new Token(TokenType.LONG, parseLong(start, position - 1), startLine, startColumn);
                // 单精度浮点型
                case 'f':
                case 'F':
                    advance();
                    return new Token(TokenType.FLOAT, parseFloat(start, position - 1), startLine, startColumn);
                // 双精度浮点型
                case 'd':
                case 'D':
                    advance();
                    return new Token(TokenType.DOUBLE, parseDouble(start, position - 1), startLine, startColumn);
                // 默认双精度
                default:
                    return new Token(TokenType.DOUBLE, parseDouble(start, position), startLine, startColumn);
            }
        } else {
            // 到达文件末尾
            if (isDouble) {
                return new Token(TokenType.DOUBLE, parseDouble(start, position), startLine, startColumn);
            } else {
                return new Token(TokenType.INTEGER, parseInteger(start, position), startLine, startColumn);
            }
        }
    }

    /**
     * 判断是否为中文字符
     *
     * @param c 要检查的字符
     * @return 是否为中文字符
     */
    private boolean isChineseChar(char c) {
        return c >= '一' && c <= '龥';
    }

    /**
     * 判断是否为字母字符
     *
     * @param c 要检查的字符
     * @return 是否为字母字符
     */
    private boolean isAlphabetChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * 判断是否为数字字符
     *
     * @param c 要检查的字符
     * @return 是否为数字字符
     */
    private boolean isNumberChar(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * 消费标识符
     *
     * @return 标识符词法单元
     */
    private Token consumeIdentifier() {
        int startLine = line;
        int startColumn = column;
        // 记录起始位置
        int start = position;
        // 消费第一个字符（已经在外部验证过是有效的标识符开始字符）
        // 接下来的字符可以是字母、数字、下划线或中文字符
        do {
            advance();
        } while (position < sourceLength && (isAlphabetChar(curr) || isNumberChar(curr) || isChineseChar(curr) || curr == '_' || curr == '-'));
        // 提取标识符
        String identifier = new String(sourceChars, start, position - start);
        return new Token(getIdentifierType(identifier), identifier, startLine, startColumn);
    }

    /**
     * 消费操作符
     *
     * @return 操作符词法单元
     */
    private Token consumeOperator() {
        int startLine = line;
        int startColumn = column;
        char first = curr;

        // 快速处理双字符操作符
        char second = next;
        if (second != '\0') {
            if (first == '=' && second == '=') {
                advance();
                advance();
                return new Token(TokenType.EQUAL, "==", startLine, startColumn);
            } else if (first == '!' && second == '=') {
                advance();
                advance();
                return new Token(TokenType.NOT_EQUAL, "!=", startLine, startColumn);
            } else if (first == '>' && second == '=') {
                advance();
                advance();
                return new Token(TokenType.GREATER_EQUAL, ">=", startLine, startColumn);
            } else if (first == '<' && second == '=') {
                advance();
                advance();
                return new Token(TokenType.LESS_EQUAL, "<=", startLine, startColumn);
            } else if (first == '+' && second == '=') {
                advance();
                advance();
                return new Token(TokenType.PLUS_ASSIGN, "+=", startLine, startColumn);
            } else if (first == '-' && second == '=') {
                advance();
                advance();
                return new Token(TokenType.MINUS_ASSIGN, "-=", startLine, startColumn);
            } else if (first == '*' && second == '=') {
                advance();
                advance();
                return new Token(TokenType.MULTIPLY_ASSIGN, "*=", startLine, startColumn);
            } else if (first == '/' && second == '=') {
                advance();
                advance();
                return new Token(TokenType.DIVIDE_ASSIGN, "/=", startLine, startColumn);
            } else if (first == '%' && second == '=') {
                advance();
                advance();
                return new Token(TokenType.MODULO_ASSIGN, "/=", startLine, startColumn);
            } else if (first == '.' && second == '.') {
                advance();
                advance();
                // 检查 ..< 操作符
                if (position < sourceLength && curr == '<') {
                    advance();
                    return new Token(TokenType.RANGE_EXCLUSIVE, "..<", startLine, startColumn);
                }
                return new Token(TokenType.RANGE, "..", startLine, startColumn);
            } else if (first == '?' && second == '.') {
                advance();
                advance();
                return new Token(TokenType.QUESTION_DOT, "?.", startLine, startColumn);
            } else if (first == '?' && second == ':') {
                advance();
                advance();
                return new Token(TokenType.QUESTION_COLON, "?:", startLine, startColumn);
            }
        }
        // 消费当前字符
        advance();
        TokenType tokenType = SINGLE_CHAR_TOKENS.getOrDefault(first, TokenType.UNKNOWN);
        return new Token(tokenType, String.valueOf(first), startLine, startColumn);
    }

    // 辅助方法 - 读取下一个字符
    private void advance() {
        position++;
        column++;
        // 更新字符缓存
        curr = position < sourceLength ? sourceChars[position] : '\0';
        next = position + 1 < sourceLength ? sourceChars[position + 1] : '\0';
    }

    private int parseInteger(int start, int end) {
        return Integer.parseInt(new String(sourceChars, start, end - start));
    }

    private long parseLong(int start, int end) {
        return Long.parseLong(new String(sourceChars, start, end - start));
    }

    private float parseFloat(int start, int end) {
        return Float.parseFloat(new String(sourceChars, start, end - start));
    }

    private double parseDouble(int start, int end) {
        return Double.parseDouble(new String(sourceChars, start, end - start));
    }
}