package org.tabooproject.fluxon.lexer;

import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompilationPhase;

import java.util.*;

/**
 * 词法分析器
 * 将源代码转换为词法单元序列
 * <p>
 * 支持字符串插值语法：{@code "Hello ${name}!"}
 * <p>
 * 字符串插值状态转换图：
 * <pre>
 *          ┌─────────────────────────────────────┐
 *          │                                     │
 *          ▼                                     │
 *     ┌─────────┐   遇到 " 或 '     ┌───────────┐ │
 *     │ NORMAL  │ ───────────────► │  STRING   │ │
 *     └─────────┘                  └───────────┘ │
 *          ▲                            │        │
 *          │                           ${        │
 *          │                            │        │
 *          │                            ▼        │
 *          │                   ┌──────────────┐  │
 *          │                   │INTERPOLATION │──┘
 *          │                   └──────────────┘ 遇到匹配的 }
 *          │                            │
 *          └────────────────────────────┘
 *               (嵌套字符串时再次入栈)
 * </pre>
 * <p>
 * Token 输出示例：
 * <ul>
 *   <li>输入: {@code "Hello ${name}!"}
 *   <li>输出: STRING_PART("Hello ") → INTERPOLATION_START → IDENTIFIER("name") → INTERPOLATION_END → STRING_PART("!")
 * </ul>
 */
public class Lexer implements CompilationPhase<List<Token>> {

    // 关键字映射表
    public static final Map<String, TokenType> KEYWORDS;
    // 单字符映射表
    public static final Map<Character, TokenType> SINGLE_CHAR_TOKENS;
    // 多字符操作符映射表，支持双字符及可选的三字符延续，单次查询完成匹配
    public static final Map<Integer, Long> MULTI_CHAR_TOKENS;
    // TokenType 枚举值缓存，避免重复调用 values()
    private static final TokenType[] TOKEN_TYPES = TokenType.values();

    private char[] sourceChars;
    private int sourceLength;
    private int position = 0;
    private int line = 1;
    private int column = 1;

    private char curr;        // 当前字符缓存
    private char next;        // 下一个字符缓存

    // 字符串插值状态栈，用于追踪嵌套的字符串上下文
    private Deque<StringContext> stringStack;

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
        KEYWORDS.put("static", TokenType.STATIC);
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

        // 初始化多字符操作符映射（双字符 + 可选三字符延续）
        MULTI_CHAR_TOKENS = new HashMap<>(16);
        // 相等性操作符（== → ===, != → !==）
        MULTI_CHAR_TOKENS.put(charPairKey('=', '='), encode(TokenType.EQUAL, '=', TokenType.IDENTICAL));
        MULTI_CHAR_TOKENS.put(charPairKey('!', '='), encode(TokenType.NOT_EQUAL, '=', TokenType.NOT_IDENTICAL));
        MULTI_CHAR_TOKENS.put(charPairKey('>', '='), encode(TokenType.GREATER_EQUAL));
        MULTI_CHAR_TOKENS.put(charPairKey('<', '='), encode(TokenType.LESS_EQUAL));
        MULTI_CHAR_TOKENS.put(charPairKey('+', '='), encode(TokenType.PLUS_ASSIGN));
        MULTI_CHAR_TOKENS.put(charPairKey('-', '='), encode(TokenType.MINUS_ASSIGN));
        MULTI_CHAR_TOKENS.put(charPairKey('*', '='), encode(TokenType.MULTIPLY_ASSIGN));
        MULTI_CHAR_TOKENS.put(charPairKey('/', '='), encode(TokenType.DIVIDE_ASSIGN));
        MULTI_CHAR_TOKENS.put(charPairKey('%', '='), encode(TokenType.MODULO_ASSIGN));
        MULTI_CHAR_TOKENS.put(charPairKey('?', '.'), encode(TokenType.QUESTION_DOT));
        MULTI_CHAR_TOKENS.put(charPairKey(':', ':'), encode(TokenType.CONTEXT_CALL));
        // 可延续的双字符操作符（.. → ..<, ?: → ?::）
        MULTI_CHAR_TOKENS.put(charPairKey('.', '.'), encode(TokenType.RANGE, '<', TokenType.RANGE_EXCLUSIVE));
        MULTI_CHAR_TOKENS.put(charPairKey('?', ':'), encode(TokenType.QUESTION_COLON, ':', TokenType.QUESTION_CONTEXT_CALL));
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
        this.stringStack = new ArrayDeque<>();

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

        while (position < sourceLength) {
            // 检查是否需要继续扫描字符串（插值结束后）
            if (!stringStack.isEmpty() && stringStack.peek().braceDepth == 0) {
                consumeStringContinuation(tokens);
                continue;
            }
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
                consumeStringStart(tokens);
            }
            // 数字检查使用优化的范围检查而不是 Character.isDigit()
            else if (c >= '0' && c <= '9') {
                tokens.add(consumeNumber());
            }
            // 标识符检查，扩展为支持中文字符
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '#' || Character.isIdeographic(c) || isChineseChar(c)) {
                tokens.add(consumeIdentifier());
            }
            // 插值模式下的大括号追踪
            else if (c == '{') {
                if (!stringStack.isEmpty()) {
                    stringStack.peek().braceDepth++;
                }
                tokens.add(consumeOperator());
            } else if (c == '}') {
                if (!stringStack.isEmpty() && stringStack.peek().braceDepth > 0) {
                    StringContext ctx = stringStack.peek();
                    ctx.braceDepth--;
                    if (ctx.braceDepth == 0) {
                        // 插值结束
                        int startLine = line;
                        int startColumn = column;
                        advance();
                        tokens.add(new Token(TokenType.INTERPOLATION_END, "}", startLine, startColumn));
                        continue;
                    }
                }
                tokens.add(consumeOperator());
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
        } while (position < sourceLength && (isAlphabetChar(curr) || isNumberChar(curr) || isChineseChar(curr) || curr == '_' || curr == '-' || curr == '$'));
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

        // 检查多字符操作符（单次查询，支持双字符 + 可选三字符延续）
        if (second != '\0') {
            Long info = MULTI_CHAR_TOKENS.get(charPairKey(first, second));
            if (info != null) {
                // 解码延续信息（高 32 位）
                int continuation = (int) (info >>> 32);
                if (continuation != 0) {
                    // 检查第三个字符是否匹配延续
                    char expectedThird = (char) (continuation >>> 16);
                    char third = position + 2 < sourceLength ? sourceChars[position + 2] : '\0';
                    if (third == expectedThird) {
                        // 匹配三字符操作符
                        advance();
                        advance();
                        advance();
                        TokenType extendedType = TOKEN_TYPES[continuation & 0xFFFF];
                        return new Token(extendedType, new String(new char[]{first, second, third}), startLine, startColumn);
                    }
                }
                // 匹配双字符操作符
                advance();
                advance();
                TokenType baseType = TOKEN_TYPES[(int) (info & 0xFFFF)];
                return new Token(baseType, new String(new char[]{first, second}), startLine, startColumn);
            }
        }

        // 检查单字符
        TokenType tokenType = SINGLE_CHAR_TOKENS.getOrDefault(first, TokenType.UNKNOWN);
        advance();
        return new Token(tokenType, String.valueOf(first), startLine, startColumn);
        // endregion
    }

    // region 消费字符串

    // 消费字符串开始（从引号开始）
    private void consumeStringStart(List<Token> tokens) {
        int startLine = line;
        int startColumn = column;
        char quote = curr;
        ScanMode mode = (quote == '"') ? ScanMode.STRING_DOUBLE : ScanMode.STRING_SINGLE;
        // 消费引号
        advance();
        // 扫描字符串内容（支持嵌套插值）
        consumeStringContent(tokens, mode, quote, startLine, startColumn);
    }

    // 插值结束后继续扫描字符串
    private void consumeStringContinuation(List<Token> tokens) {
        StringContext ctx = Objects.requireNonNull(stringStack.peek());
        char quote = (ctx.mode == ScanMode.STRING_DOUBLE) ? '"' : '\'';
        int startLine = line;
        int startColumn = column;
        consumeStringContent(tokens, ctx.mode, quote, startLine, startColumn);
    }

    // 扫描字符串内容（共用逻辑，支持嵌套插值）
    private void consumeStringContent(List<Token> tokens, ScanMode mode, char quote, int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder(Math.min(32, sourceLength - position));
        while (position < sourceLength && curr != quote) {
            char c = this.curr;
            // 检测插值开始: ${
            if (c == '$' && next == '{') {
                // 输出当前累积的字符串片段
                if (sb.length() > 0) {
                    tokens.add(new Token(TokenType.STRING_PART, sb.toString(), startLine, startColumn));
                    sb.setLength(0);
                }
                // 输出插值开始标记
                int interpLine = line;
                int interpColumn = column;
                advance(); // 消费 $
                advance(); // 消费 {
                tokens.add(new Token(TokenType.INTERPOLATION_START, "${", interpLine, interpColumn));
                // 压入或复用字符串上下文
                // 只有当栈为空或栈顶在另一个插值内部（嵌套字符串）时，才压入新上下文
                // 同一字符串的多个插值复用同一个上下文
                if (stringStack.isEmpty() || stringStack.peek().braceDepth > 0) {
                    stringStack.push(new StringContext(mode));
                }
                stringStack.peek().braceDepth = 1;
                return; // 返回主循环处理插值内容
            }
            // 转义处理
            if (!appendCharOrEscape(sb)) {
                break;
            }
        }
        // 字符串结束
        int endLine = line;
        int endColumn = column;
        if (position < sourceLength) {
            advance(); // 消费结束引号
            endColumn = column;
        }
        // 根据调用来源选择 token 类型
        if (!stringStack.isEmpty() && stringStack.peek().braceDepth == 0) {
            // 通过 consumeStringContinuation 调用，是插值字符串的最后部分
            if (sb.length() > 0) {
                tokens.add(new Token(TokenType.STRING_PART, sb.toString(), startLine, startColumn, endLine, endColumn));
            }
            // 字符串完全结束，弹出上下文
            stringStack.pop();
        } else {
            // 普通字符串（无插值）或插值内部的独立字符串
            tokens.add(new Token(TokenType.STRING, sb.toString(), startLine, startColumn, endLine, endColumn));
        }
    }

    // 处理字符或转义序列，追加到 StringBuilder
    // 返回 false 表示遇到文件末尾，应中断循环
    private boolean appendCharOrEscape(StringBuilder sb) {
        char c = this.curr;
        if (c == '\\') {
            advance();
            if (position >= sourceLength) {
                return false;
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
                case '$':
                    sb.append('$');
                    break;
                default:
                    sb.append(escaped);
            }
        } else {
            sb.append(c);
            advance();
        }
        return true;
    }

    // endregion

    // 消费数字
    private Token consumeNumber() {
        // region
        int startLine = line;
        int startColumn = column;
        int start = position;
        boolean isDouble = false;

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

    // 辅助方法 - 将两个字符组合为 int 复合键
    private static int charPairKey(char first, char second) {
        return (first << 16) | second;
    }

    // 编码双字符操作符（无延续）
    private static long encode(TokenType baseType) {
        return baseType.ordinal();
    }

    /**
     * 编码双字符操作符（有三字符延续）
     * <pre>
     * 64-bit 编码格式:
     * ┌───────────────────────────────┬──────────────────┐
     * │ 高 32 位: 延续信息                低 16 位: 基础类型
     * │ [thirdChar:16][extType:16]      [baseType:16]
     * └───────────────────────────────┴──────────────────┘
     * </pre>
     */
    private static long encode(TokenType baseType, char thirdChar, TokenType extendedType) {
        long continuation = ((long) thirdChar << 16) | extendedType.ordinal();
        return (continuation << 32) | baseType.ordinal();
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
