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
    private final String source;
    private int position = 0;
    private final int sourceLength; // 缓存源代码长度，避免重复调用
    private int line = 1;
    private int column = 1;
    private char currentChar; // 当前字符缓存
    private char nextChar;    // 下一个字符缓存

    // 关键字映射表
    private static final Map<String, TokenType> KEYWORDS;

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
        KEYWORDS.put("when", TokenType.WHEN);
        KEYWORDS.put("is", TokenType.IS);
        KEYWORDS.put("in", TokenType.IN);
        KEYWORDS.put("async", TokenType.ASYNC);
        KEYWORDS.put("await", TokenType.AWAIT);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("try", TokenType.TRY);
        KEYWORDS.put("catch", TokenType.CATCH);

        // 布尔值特殊处理
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
    }

    /**
     * 创建词法分析器
     *
     * @param source 源代码
     */
    public Lexer(String source) {
        this.source = source;
        this.sourceLength = source.length();

        // 预加载第一个字符，避免重复边界检查
        if (sourceLength > 0) {
            currentChar = source.charAt(0);
            nextChar = sourceLength > 1 ? source.charAt(1) : '\0';
        } else {
            currentChar = '\0';
            nextChar = '\0';
        }
    }

    /**
     * 执行词法分析
     *
     * @param context 编译上下文
     * @return 词法单元列表
     */
    @Override
    public List<Token> process(CompilationContext context) {
        return tokenize();
    }

    /**
     * 将源代码转换为词法单元序列
     *
     * @return 词法单元列表
     */
    public List<Token> tokenize() {
        // 预分配合适大小的列表，避免频繁扩容
        List<Token> tokens = new ArrayList<>(Math.max(512, sourceLength / 8));

        // 重置状态
        position = 0;
        line = 1;
        column = 1;

        // 预加载第一个字符
        if (sourceLength > 0) {
            currentChar = source.charAt(0);
            nextChar = sourceLength > 1 ? source.charAt(1) : '\0';
        } else {
            currentChar = '\0';
            nextChar = '\0';
        }

        while (position < sourceLength) {
            // 使用缓存的字符，避免调用peek()
            char c = currentChar;

            // 使用内联的方式处理常见情况，减少方法调用开销
            if (c <= ' ') { // 快速空白字符检查
                consumeWhitespace();
            }
            // 处理双字符逻辑操作符
            else if (c == '&' && nextChar == '&') {
                // 处理 && 操作符
                int startLine = line;
                int startColumn = column;
                advance(); // 消费第一个 &
                advance(); // 消费第二个 &
                tokens.add(new Token(TokenType.AND, AND_STRING, startLine, startColumn));
            } else if (c == '|' && nextChar == '|') {
                // 处理 || 操作符
                int startLine = line;
                int startColumn = column;
                advance(); // 消费第一个 |
                advance(); // 消费第二个 |
                tokens.add(new Token(TokenType.OR, OR_STRING, startLine, startColumn));
            } else if (c == '-' && nextChar == '>') {
                // 处理 -> 操作符
                int startLine = line;
                int startColumn = column;
                advance(); // 消费 -
                advance(); // 消费 >
                tokens.add(new Token(TokenType.ARROW, ARROW_STRING, startLine, startColumn));
            } else if (c == '/' && nextChar == '/') {
                // 行注释 - 快速消费
                consumeLineComment();
            } else if (c == '/' && nextChar == '*') {
                // 块注释 - 快速消费
                consumeBlockComment();
            } else if (c == '"' || c == '\'') {
                tokens.add(consumeString());
            }
            // 数字检查使用优化的范围检查而不是Character.isDigit()
            else if (c >= '0' && c <= '9') {
                tokens.add(consumeNumber());
            }
            // 标识符检查
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') {
                tokens.add(consumeIdentifier());
            } else if (c == '&') {
                // 处理单个 & 字符
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
        while (position < sourceLength && currentChar <= ' ') {
            char c = currentChar;
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
        } while (position < sourceLength && currentChar != '\n');
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
            if (currentChar == '*' && nextChar == '/') {
                advance(); // 跳过 *
                advance(); // 跳过 /
                break;
            }
            if (currentChar == '\n') {
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
        char quote = currentChar;
        advance(); // 消费引号

        // 使用 StringBuilder，避免中间字符串创建
        StringBuilder sb = new StringBuilder(Math.min(32, sourceLength - position));

        while (position < sourceLength && currentChar != quote) {
            char c = currentChar;

            if (c == '\\') {
                advance(); // 消费反斜杠

                if (position >= sourceLength) {
                    break;
                }

                char escaped = currentChar;
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
        boolean isFloat = false;

        // 消费整数部分 - 使用字符范围检查代替 Character.isDigit()
        while (position < sourceLength && currentChar >= '0' && currentChar <= '9') {
            advance();
        }

        // 检查是否有小数点
        if (position < sourceLength && currentChar == '.') {
            isFloat = true;
            // 消费小数部分 - 使用字符范围检查
            do {
                advance();
            } while (position < sourceLength && currentChar >= '0' && currentChar <= '9');
        }

        // 提前处理简单情况，减少后续不必要的检查
        if (!isFloat && position < sourceLength && currentChar != 'e' && currentChar != 'E') {
            // 优化纯整数情况，直接提取子字符串
            String value = source.substring(start, position);
            return new Token(TokenType.INTEGER, value, startLine, startColumn);
        }

        // 检查是否有指数部分
        if (position < sourceLength && (currentChar == 'e' || currentChar == 'E')) {
            isFloat = true;
            advance(); // 消费 e 或 E
            // 检查指数符号
            if (position < sourceLength && (currentChar == '+' || currentChar == '-')) {
                advance(); // 消费 + 或 -
            }
            // 消费指数部分
            while (position < sourceLength && currentChar >= '0' && currentChar <= '9') {
                advance(); // 消费数字
            }
        }
        // 处理 float 或科学计数法
        TokenType type = isFloat ? TokenType.FLOAT : TokenType.INTEGER;
        return new Token(type, source.substring(start, position), startLine, startColumn);
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
        // 接下来的字符可以是字母、数字或下划线，使用优化的检查
        do {
            advance();
        } while (position < sourceLength && ((currentChar >= 'a' && currentChar <= 'z') || (currentChar >= 'A' && currentChar <= 'Z') || (currentChar >= '0' && currentChar <= '9') || currentChar == '_'));

        // 提取标识符
        String identifier = source.substring(start, position);

        // 获取标识符类型
        TokenType type = getIdentifierType(identifier);
        return new Token(type, identifier, startLine, startColumn);
    }

    /**
     * 消费操作符
     *
     * @return 操作符词法单元
     */
    private Token consumeOperator() {
        int startLine = line;
        int startColumn = column;
        char first = currentChar;

        // 快速处理双字符操作符
        char second = nextChar;
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
            } else if (first == '.' && second == '.') {
                advance();
                advance();
                // 检查 ..< 操作符
                if (position < sourceLength && currentChar == '<') {
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

        // 使用单字符字符串，避免每次创建新字符串
        String value = String.valueOf(first);

        // 使用快速查找
        return switch (first) {
            case '+' -> new Token(TokenType.PLUS, "+", startLine, startColumn);
            case '-' -> new Token(TokenType.MINUS, "-", startLine, startColumn);
            case '*' -> new Token(TokenType.MULTIPLY, "*", startLine, startColumn);
            case '/' -> new Token(TokenType.DIVIDE, "/", startLine, startColumn);
            case '%' -> new Token(TokenType.MODULO, "%", startLine, startColumn);
            case '=' -> new Token(TokenType.ASSIGN, "=", startLine, startColumn);
            case '>' -> new Token(TokenType.GREATER, ">", startLine, startColumn);
            case '<' -> new Token(TokenType.LESS, "<", startLine, startColumn);
            case '!' -> new Token(TokenType.NOT, "!", startLine, startColumn);
            case '.' -> new Token(TokenType.DOT, ".", startLine, startColumn);
            case ',' -> new Token(TokenType.COMMA, ",", startLine, startColumn);
            case ':' -> new Token(TokenType.COLON, ":", startLine, startColumn);
            case ';' -> new Token(TokenType.SEMICOLON, ";", startLine, startColumn);
            case '(' -> new Token(TokenType.LEFT_PAREN, "(", startLine, startColumn);
            case ')' -> new Token(TokenType.RIGHT_PAREN, ")", startLine, startColumn);
            case '{' -> new Token(TokenType.LEFT_BRACE, "{", startLine, startColumn);
            case '}' -> new Token(TokenType.RIGHT_BRACE, "}", startLine, startColumn);
            case '[' -> new Token(TokenType.LEFT_BRACKET, "[", startLine, startColumn);
            case ']' -> new Token(TokenType.RIGHT_BRACKET, "]", startLine, startColumn);
            case '?' -> new Token(TokenType.QUESTION, "?", startLine, startColumn);
            case '|' -> new Token(TokenType.UNKNOWN, "|", startLine, startColumn); // 处理单个 |
            default -> new Token(TokenType.UNKNOWN, value, startLine, startColumn);
        };
    }

    /**
     * 消费当前字符并前进
     */
    private void advance() {
        position++;
        column++;
        // 更新字符缓存
        currentChar = position < sourceLength ? source.charAt(position) : '\0';
        nextChar = position + 1 < sourceLength ? source.charAt(position + 1) : '\0';
    }
}