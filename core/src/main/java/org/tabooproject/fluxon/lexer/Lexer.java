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

    // 关键字映射表
    public static final Map<String, TokenType> KEYWORDS;
    // 单字符映射表
    public static final Map<Character, TokenType> SINGLE_CHAR_TOKENS;
    // 双字符映射表
    public static final Map<String, TokenType> DOUBLE_CHAR_TOKENS;

    private char[] sourceChars;
    private int sourceLength;
    private int position = 0;
    private int line = 1;
    private int column = 1;

    private char curr;        // 当前字符缓存
    private char next;        // 下一个字符缓存

    static {
        // region
        // 初始化关键字映射
        KEYWORDS = new HashMap<>(32);
        KEYWORDS.put("import", TokenType.IMPORT);
        KEYWORDS.put("def", TokenType.DEF);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("then", TokenType.THEN);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("when", TokenType.WHEN);
        KEYWORDS.put("is", TokenType.IS);
        KEYWORDS.put("in", TokenType.IN);
        KEYWORDS.put("sync", TokenType.SYNC);
        KEYWORDS.put("async", TokenType.ASYNC);
        KEYWORDS.put("await", TokenType.AWAIT);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("try", TokenType.TRY);
        KEYWORDS.put("catch", TokenType.CATCH);
        KEYWORDS.put("finally", TokenType.FINALLY);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("break", TokenType.BREAK);
        KEYWORDS.put("continue", TokenType.CONTINUE);
        KEYWORDS.put("new", TokenType.NEW);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("null", TokenType.NULL);

        // 初始化单字符映射
        SINGLE_CHAR_TOKENS = new HashMap<>(32);
        SINGLE_CHAR_TOKENS.put('&', TokenType.AMPERSAND);
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
        SINGLE_CHAR_TOKENS.put('|', TokenType.PIPE);
        SINGLE_CHAR_TOKENS.put('@', TokenType.AT);

        // 初始化双字符映射
        DOUBLE_CHAR_TOKENS = new HashMap<>(32);
        DOUBLE_CHAR_TOKENS.put("==", TokenType.EQUAL);
        DOUBLE_CHAR_TOKENS.put("!=", TokenType.NOT_EQUAL);
        DOUBLE_CHAR_TOKENS.put(">=", TokenType.GREATER_EQUAL);
        DOUBLE_CHAR_TOKENS.put("<=", TokenType.LESS_EQUAL);
        DOUBLE_CHAR_TOKENS.put("+=", TokenType.PLUS_ASSIGN);
        DOUBLE_CHAR_TOKENS.put("-=", TokenType.MINUS_ASSIGN);
        DOUBLE_CHAR_TOKENS.put("*=", TokenType.MULTIPLY_ASSIGN);
        DOUBLE_CHAR_TOKENS.put("/=", TokenType.DIVIDE_ASSIGN);
        DOUBLE_CHAR_TOKENS.put("%=", TokenType.MODULO_ASSIGN);
        DOUBLE_CHAR_TOKENS.put("?:", TokenType.QUESTION_COLON);
        DOUBLE_CHAR_TOKENS.put("..", TokenType.RANGE);
        DOUBLE_CHAR_TOKENS.put("::", TokenType.CONTEXT_CALL);
        // endregion
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

    // 词法分析主逻辑
    // 将源代码转换为词法单元序列
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
            // 使用缓存的字符，避免调用 peek()
            char c = curr;
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
                tokens.add(new Token(TokenType.AND, "&&", startLine, startColumn));
            } else if (c == '|' && next == '|') {
                // 处理 || 操作符
                int startLine = line;
                int startColumn = column;
                advance(); // 消费第一个 |
                advance(); // 消费第二个 |
                tokens.add(new Token(TokenType.OR, "||", startLine, startColumn));
            } else if (c == '-' && next == '>') {
                // 处理 -> 操作符
                int startLine = line;
                int startColumn = column;
                advance(); // 消费 -
                advance(); // 消费 >
                tokens.add(new Token(TokenType.ARROW, "->", startLine, startColumn));
            } else if (c == '/' && next == '/') {
                // 行注释 - 快速消费
                consumeLineComment();
            } else if (c == '/' && next == '*') {
                // 块注释 - 快速消费
                consumeBlockComment();
            } else if (c == '"' || c == '\'') {
                tokens.add(consumeString());
            }
            // 数字检查使用优化的范围检查而不是 Character.isDigit()
            else if (c >= '0' && c <= '9') {
                tokens.add(consumeNumber());
            }
            // 标识符检查，扩展为支持中文字符
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || Character.isIdeographic(c) || isChineseChar(c)) {
                tokens.add(consumeIdentifier());
            } else {
                tokens.add(consumeOperator());
            }
        }

        // 添加 EOF 标记
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    // 消费空白字符
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

    // 消费行注释
    private void consumeLineComment() {
        // 跳过 //
        advance();
        // 消费直到行尾
        do {
            advance();
        } while (position < sourceLength && curr != '\n');
    }

    // 消费块注释
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

    // 消费标识符
    private Token consumeIdentifier() {
        // region
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
        // endregion
    }

    // 消费操作符
    private Token consumeOperator() {
        // region
        int startLine = line;
        int startColumn = column;
        char first = curr;
        char second = next;

        // 检查双字符
        if (second != '\0') {
            String pair = String.valueOf(first) + second;
            TokenType tokenType = DOUBLE_CHAR_TOKENS.get(pair);
            if (tokenType != null) {
                advance();
                advance();
                // 处理 ..< 三字符操作符
                if (tokenType == TokenType.RANGE && curr == '<') {
                    advance();
                    return new Token(TokenType.RANGE_EXCLUSIVE, "..<", startLine, startColumn);
                }
                return new Token(tokenType, pair, startLine, startColumn);
            }
        }

        // 检查单字符
        TokenType tokenType = SINGLE_CHAR_TOKENS.getOrDefault(first, TokenType.UNKNOWN);
        advance();
        return new Token(tokenType, String.valueOf(first), startLine, startColumn);
        // endregion
    }

    // 消费字符串
    private Token consumeString() {
        // region
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
        // endregion
    }

    // 消费数字
    private Token consumeNumber() {
        // region
        int startLine = line;
        int startColumn = column;
        int start = position;
        boolean isDouble = false;

        // 检查是否为负数
        if (curr == '-') {
            advance(); // 消费负号
        }

        // 消费整数部分 - 使用字符范围检查代替 Character.isDigit()，支持下划线分隔符
        while (position < sourceLength && (curr >= '0' && curr <= '9')) {
            // 跳过下划线分隔符
            do {
                advance();
            } while (position < sourceLength && curr == '_');
        }

        // 检查是否有小数点
        if (position < sourceLength && curr == '.') {
            // 检查下一个字符是否也是小数点（范围操作符的开始）
            if (next == '.') {
                // 这是范围操作符的开始，不是浮点数的一部分
                // 尝试解析为 int，超出范围则自动升级为 long
                Integer intValue = parseIntegerSafe(start, position);
                if (intValue != null) {
                    return new Token(TokenType.INTEGER, intValue, startLine, startColumn);
                } else {
                    return new Token(TokenType.LONG, parseLong(start, position), startLine, startColumn);
                }
            } else {
                isDouble = true;
                // 消费小数点
                advance();
                // 消费小数部分 - 使用字符范围检查，支持下划线分隔符
                while (position < sourceLength && curr >= '0' && curr <= '9') {
                    // 跳过下划线分隔符
                    do {
                        advance();
                    } while (position < sourceLength && curr == '_');
                }
            }
        }

        // 提前处理简单情况，减少后续不必要的检查
        if (!isDouble && position < sourceLength && curr != 'e' && curr != 'E') {
            // 检查是否为LONG类型（带L或l后缀）
            if (curr == 'L' || curr == 'l') {
                advance(); // 消费 L 或 l
                return new Token(TokenType.LONG, parseLong(start, position - 1), startLine, startColumn);
            }
            // 检查是否为FLOAT类型（带f或F后缀）
            if (curr == 'f' || curr == 'F') {
                advance(); // 消费 f 或 F
                return new Token(TokenType.FLOAT, parseFloat(start, position - 1), startLine, startColumn);
            }
            // 优化纯整数情况，尝试解析为 int，超出范围则自动升级为 long
            Integer intValue = parseIntegerSafe(start, position);
            if (intValue != null) {
                return new Token(TokenType.INTEGER, intValue, startLine, startColumn);
            } else {
                return new Token(TokenType.LONG, parseLong(start, position), startLine, startColumn);
            }
        }

        // 检查是否有指数部分
        if (position < sourceLength && (curr == 'e' || curr == 'E')) {
            isDouble = true;
            advance(); // 消费 e 或 E
            // 检查指数符号
            if (position < sourceLength && (curr == '+' || curr == '-')) {
                advance(); // 消费 + 或 -
            }
            // 消费指数部分，指数部分不支持下划线分隔符
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
                // 尝试解析为 int，超出范围则自动升级为 long
                Integer intValue = parseIntegerSafe(start, position);
                if (intValue != null) {
                    return new Token(TokenType.INTEGER, intValue, startLine, startColumn);
                } else {
                    return new Token(TokenType.LONG, parseLong(start, position), startLine, startColumn);
                }
            }
        }
        // endregion
    }

    // 辅助方法 - 读取下一个字符
    private void advance() {
        position++;
        column++;
        // 更新字符缓存
        curr = position < sourceLength ? sourceChars[position] : '\0';
        next = position + 1 < sourceLength ? sourceChars[position + 1] : '\0';
    }

    // 辅助方法 - 判断是否为中文字符
    private boolean isChineseChar(char c) {
        return c >= '一' && c <= '龥';
    }

    // 辅助方法 - 判断是否为字母字符
    private boolean isAlphabetChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    // 辅助方法 - 判断是否为数字字符
    private boolean isNumberChar(char c) {
        return c >= '0' && c <= '9';
    }

    // 辅助方法 - 从缓冲区中提取整数，如果超出 int 范围则返回 null
    private Integer parseIntegerSafe(int start, int end) {
        String numStr = removeUnderscores(new String(sourceChars, start, end - start));
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 辅助方法 - 从缓冲区中提取整数（向后兼容）
    private int parseInteger(int start, int end) {
        return Integer.parseInt(removeUnderscores(new String(sourceChars, start, end - start)));
    }

    // 辅助方法 - 从缓冲区中提取长整数
    private long parseLong(int start, int end) {
        return Long.parseLong(removeUnderscores(new String(sourceChars, start, end - start)));
    }

    // 辅助方法 - 从缓冲区中提取单精度浮点数
    private float parseFloat(int start, int end) {
        return Float.parseFloat(removeUnderscores(new String(sourceChars, start, end - start)));
    }

    // 辅助方法 - 从缓冲区中提取双精度浮点数
    private double parseDouble(int start, int end) {
        return Double.parseDouble(removeUnderscores(new String(sourceChars, start, end - start)));
    }

    // 辅助方法 - 移除数字中的下划线
    private String removeUnderscores(String number) {
        // 快速检查是否包含下划线，避免不必要的处理
        if (number.indexOf('_') == -1) {
            return number;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (c != '_') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // 辅助方法 - 根据标识符获取类型
    private TokenType getIdentifierType(String identifier) {
        return KEYWORDS.getOrDefault(identifier, TokenType.IDENTIFIER);
    }
}
