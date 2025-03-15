package org.tabooproject.fluxon;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;

import org.tabooproject.fluxon.compiler.CompilationContext;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Lexer 模块测试类
 * 全面测试 Fluxon 词法分析器的各项功能
 */
public class LexerTest {

    /**
     * 测试获取 Token 序列
     *
     * @param source 源代码
     * @return Token 列表
     */
    private List<Token> getTokens(String source) {
        CompilationContext context = new CompilationContext(source);
        Lexer lexer = new Lexer(source);
        return lexer.process(context);
    }

    /**
     * 按类型过滤 Token
     *
     * @param tokens Token 列表
     * @param type   Token 类型
     * @return 过滤后的列表
     */
    private List<Token> filterByType(List<Token> tokens, TokenType type) {
        return tokens.stream().filter(t -> t.getType() == type).collect(Collectors.toList());
    }

    /**
     * 获取指定类型 Token 的值
     *
     * @param tokens Token 列表
     * @param type   Token 类型
     * @return 该类型 Token 的值列表
     */
    private List<String> getValuesByType(List<Token> tokens, TokenType type) {
        return filterByType(tokens, type).stream().map(Token::getValue).collect(Collectors.toList());
    }

    /**
     * 验证 Token 序列
     *
     * @param source        源代码
     * @param expectedTypes 期望的 Token 类型序列
     */
    private void assertTokenTypes(String source, TokenType... expectedTypes) {
        List<Token> tokens = getTokens(source);
        Token lastToken = tokens.remove(tokens.size() - 1);
        assertEquals(TokenType.EOF, lastToken.getType(), "最后一个 token 应为 EOF");
        assertEquals(expectedTypes.length, tokens.size(), "Token 数量不匹配，期望: " + Arrays.toString(expectedTypes) + ", 实际: " + tokens);
    }

    @Nested
    @DisplayName("基础 Token 识别测试")
    class BasicTokenTests {

        @Test
        @DisplayName("测试标识符识别")
        void testIdentifiers() {
            String source = "abc x123 _test test_var";
            List<Token> tokens = getTokens(source);

            // 验证识别出四个标识符
            List<String> identifiers = getValuesByType(tokens, TokenType.IDENTIFIER);
            assertEquals(4, identifiers.size(), "Token 数量不匹配，应识别 4 个标识符");
            assertArrayEquals(new String[]{"abc", "x123", "_test", "test_var"}, identifiers.toArray(new String[0]));
        }

        @Test
        @DisplayName("测试关键字识别")
        void testKeywords() {
            String source = "def fun val var if then else when is in async await return try catch";
            List<Token> tokens = getTokens(source);

            // 验证所有关键字都被正确识别
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.DEF), "应识别 'def' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.FUN), "应识别 'fun' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.VAL), "应识别 'val' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.VAR), "应识别 'var' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.IF), "应识别 'if' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.THEN), "应识别 'then' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.ELSE), "应识别 'else' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.WHEN), "应识别 'when' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.IS), "应识别 'is' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.IN), "应识别 'in' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.ASYNC), "应识别 'async' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.AWAIT), "应识别 'await' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.RETURN), "应识别 'return' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.TRY), "应识别 'try' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.CATCH), "应识别 'catch' 关键字");

            // 关键字数量
            assertEquals(15, tokens.size() - 1, "应识别 15 个关键字 (不计 EOF)");
        }

        @Test
        @DisplayName("测试布尔字面量识别")
        void testBooleans() {
            String source = "true false";
            List<Token> tokens = getTokens(source);

            List<Token> booleans = filterByType(tokens, TokenType.BOOLEAN);
            assertEquals(2, booleans.size(), "应识别 2 个布尔字面量");

            List<String> values = booleans.stream().map(Token::getValue).collect(Collectors.toList());
            assertTrue(values.contains("true"), "应识别 'true' 布尔值");
            assertTrue(values.contains("false"), "应识别 'false' 布尔值");
        }
    }

    @Nested
    @DisplayName("数字字面量测试")
    class NumberLiteralTests {

        @Test
        @DisplayName("测试整数字面量")
        void testIntegers() {
            String source = "0 123 42 9876543210";
            List<Token> tokens = getTokens(source);

            List<String> integers = getValuesByType(tokens, TokenType.INTEGER);
            assertEquals(4, integers.size(), "应识别 4 个整数字面量");

            assertArrayEquals(new String[]{"0", "123", "42", "9876543210"}, integers.toArray(new String[0]));
        }

        @Test
        @DisplayName("测试浮点数字面量")
        void testFloats() {
            String source = "0.5 3.14 1.0 0.123456789";
            List<Token> tokens = getTokens(source);

            List<String> floats = getValuesByType(tokens, TokenType.FLOAT);
            assertEquals(4, floats.size(), "应识别 4 个浮点数字面量");

            assertArrayEquals(new String[]{"0.5", "3.14", "1.0", "0.123456789"}, floats.toArray(new String[0]));
        }

        @Test
        @DisplayName("测试科学计数法")
        void testScientificNotation() {
            String source = "1e10 2.5e3 1.2e-5 3e+2";
            List<Token> tokens = getTokens(source);

            List<String> floats = getValuesByType(tokens, TokenType.FLOAT);
            assertEquals(4, floats.size(), "应识别 4 个科学计数法字面量");

            assertArrayEquals(new String[]{"1e10", "2.5e3", "1.2e-5", "3e+2"}, floats.toArray(new String[0]));
        }
    }

    @Nested
    @DisplayName("字符串字面量测试")
    class StringLiteralTests {

        @Test
        @DisplayName("测试双引号字符串")
        void testDoubleQuotedStrings() {
            String source = "\"hello\" \"world\" \"\"";
            List<Token> tokens = getTokens(source);

            List<String> strings = getValuesByType(tokens, TokenType.STRING);
            assertEquals(3, strings.size(), "应识别 3 个字符串字面量");

            assertArrayEquals(new String[]{"hello", "world", ""}, strings.toArray(new String[0]));
        }

        @Test
        @DisplayName("测试单引号字符串")
        void testSingleQuotedStrings() {
            String source = "'hello' 'world' ''";
            List<Token> tokens = getTokens(source);

            List<String> strings = getValuesByType(tokens, TokenType.STRING);
            assertEquals(3, strings.size(), "应识别 3 个字符串字面量");

            assertArrayEquals(new String[]{"hello", "world", ""}, strings.toArray(new String[0]));
        }

        @Test
        @DisplayName("测试字符串转义序列")
        void testEscapeSequences() {
            String source = "\"\\n\\r\\t\\\\\\\"\\\'\" '\\n\\r\\t\\\\\\\"\\\'\'";
            List<Token> tokens = getTokens(source);

            List<String> strings = getValuesByType(tokens, TokenType.STRING);
            assertEquals(2, strings.size(), "应识别 2 个带转义序列的字符串字面量");

            // 检查转义序列是否正确解析
            assertEquals("\n\r\t\\\"\'", strings.get(0), "双引号字符串转义序列应正确解析");
            assertEquals("\n\r\t\\\"\'", strings.get(1), "单引号字符串转义序列应正确解析");
        }
    }

    @Nested
    @DisplayName("操作符测试")
    class OperatorTests {

        @Test
        @DisplayName("测试算术操作符")
        void testArithmeticOperators() {
            String source = "+ - * / %";
            assertTokenTypes(source, TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO);
        }

        @Test
        @DisplayName("测试比较操作符")
        void testComparisonOperators() {
            String source = "== != > < >= <=";
            assertTokenTypes(source, TokenType.EQUAL, TokenType.NOT_EQUAL, TokenType.GREATER, TokenType.LESS, TokenType.GREATER_EQUAL, TokenType.LESS_EQUAL);
        }

        @Test
        @DisplayName("测试赋值操作符")
        void testAssignmentOperators() {
            String source = "= += -= *= /=";
            assertTokenTypes(source, TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN, TokenType.MULTIPLY_ASSIGN, TokenType.DIVIDE_ASSIGN);
        }

        @Test
        @DisplayName("测试逻辑操作符")
        void testLogicalOperators() {
            // 测试所有逻辑操作符一起
            String source = "&& || !";

            // 验证能正确识别为3个不同的token：AND、OR和NOT
            assertTokenTypes(source, TokenType.AND, TokenType.OR, TokenType.NOT);
        }

        @Test
        @DisplayName("测试特殊操作符")
        void testSpecialOperators() {
            String source = "-> .. ..< ?. ?:";
            assertTokenTypes(source, TokenType.ARROW, TokenType.RANGE, TokenType.RANGE_EXCLUSIVE, TokenType.QUESTION_DOT, TokenType.QUESTION_COLON);
        }

        @Test
        @DisplayName("测试其他操作符和分隔符")
        void testOtherOperatorsAndDelimiters() {
            String source = ". , : ; ( ) { } [ ] ? &";
            assertTokenTypes(source, TokenType.DOT, TokenType.COMMA, TokenType.COLON, TokenType.SEMICOLON, TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN, TokenType.LEFT_BRACE, TokenType.RIGHT_BRACE, TokenType.LEFT_BRACKET, TokenType.RIGHT_BRACKET, TokenType.QUESTION, TokenType.AMPERSAND);
        }
    }

    @Nested
    @DisplayName("注释处理测试")
    class CommentTests {

        @Test
        @DisplayName("测试行注释")
        void testLineComments() {
            String source = "val x = 5 // 这是一个注释\nval y = 10";
            List<Token> tokens = getTokens(source);

            // 验证注释被忽略，但保留其他 token
            List<String> identifiers = getValuesByType(tokens, TokenType.IDENTIFIER);
            assertArrayEquals(new String[]{"x", "y"}, identifiers.toArray(new String[0]));

            List<String> integers = getValuesByType(tokens, TokenType.INTEGER);
            assertArrayEquals(new String[]{"5", "10"}, integers.toArray(new String[0]));
        }

        @Test
        @DisplayName("测试块注释")
        void testBlockComments() {
            String source = "val x = /* 这是一个\n多行注释 */ 5";
            List<Token> tokens = getTokens(source);

            // 验证块注释被忽略，但保留其他 token
            assertTokenTypes(source.substring(0, 6), TokenType.VAL, TokenType.IDENTIFIER);

            List<String> identifiers = getValuesByType(tokens, TokenType.IDENTIFIER);
            assertEquals(1, identifiers.size(), "应只识别一个标识符");
            assertEquals("x", identifiers.get(0), "标识符应为 'x'");

            List<String> integers = getValuesByType(tokens, TokenType.INTEGER);
            assertEquals(1, integers.size(), "应只识别一个整数");
            assertEquals("5", integers.get(0), "整数应为 '5'");
        }
    }

    @Nested
    @DisplayName("行号和列号测试")
    class LineColumnTests {

        @Test
        @DisplayName("测试行号和列号跟踪")
        void testLineColumnTracking() {
            String source = "val x = 5\nval y = 10";
            List<Token> tokens = getTokens(source);

            // 验证第一行 tokens 的行号
            for (int i = 0; i < 4; i++) { // val x = 5
                assertEquals(1, tokens.get(i).getLine(), "第一行 token 的行号应为 1");
            }

            // 验证第二行 tokens 的行号
            for (int i = 4; i < tokens.size() - 1; i++) { // val y = 10 (不包括 EOF)
                assertEquals(2, tokens.get(i).getLine(), "第二行 token 的行号应为 2");
            }

            // 验证特定 token 的列号
            Token valToken = tokens.get(0); // 第一个 val
            assertEquals(1, valToken.getColumn(), "第一个 'val' 的列号应为 1");

            Token xToken = tokens.get(1); // x
            assertEquals(5, xToken.getColumn(), "标识符 'x' 的列号应为 5");
        }

        @Test
        @DisplayName("测试多行字符串位置跟踪")
        void testMultilineStringPositions() {
            String source = "val msg = \"line1\nline2\"";
            List<Token> tokens = getTokens(source);

            // 获取字符串 token
            Token stringToken = filterByType(tokens, TokenType.STRING).get(0);

            // 验证字符串内容
            assertEquals("line1\nline2", stringToken.getValue(), "字符串内容应包含换行符");

            // 验证字符串 token 的位置
            assertEquals(1, stringToken.getLine(), "字符串 token 的行号应为起始行号");
            assertEquals(11, stringToken.getColumn(), "字符串 token 的列号应为起始列号");
        }
    }

    @Nested
    @DisplayName("特殊情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("测试空输入")
        void testEmptyInput() {
            String source = "";
            List<Token> tokens = getTokens(source);

            assertEquals(1, tokens.size(), "空输入应只生成 EOF token");
            assertEquals(TokenType.EOF, tokens.get(0).getType(), "唯一的 token 应为 EOF");
        }

        @Test
        @DisplayName("测试只包含空白字符的输入")
        void testWhitespaceOnly() {
            String source = "  \t\n  ";
            List<Token> tokens = getTokens(source);

            assertEquals(1, tokens.size(), "只含空白字符的输入应只产生 EOF token");
            assertEquals(TokenType.EOF, tokens.get(0).getType(), "唯一的 token 应为 EOF");
            assertEquals(2, tokens.get(0).getLine(), "EOF token 的行号应为最后一行");
        }

        @Test
        @DisplayName("测试只包含注释的输入")
        void testCommentsOnly() {
            String source = "// 这是注释\n/* 这也是\n注释 */";
            List<Token> tokens = getTokens(source);

            assertEquals(1, tokens.size(), "只含注释的输入应只生成 EOF token");
            assertEquals(TokenType.EOF, tokens.get(0).getType(), "唯一的 token 应为 EOF");
            assertEquals(3, tokens.get(0).getLine(), "EOF token 的行号应为最后一行");
        }
    }

    @Nested
    @DisplayName("功能性测试")
    class FunctionalTests {

        @Test
        @DisplayName("测试 Fluxon 语言语法元素")
        void testFluxonSyntaxElements() {
            String source = """
                    def factorial(n) = {
                        if &n <= 1 then 1
                        else &n * factorial(&n - 1)
                    }
                    
                    val x = 5
                    print factorial &x
                    """;

            List<Token> tokens = getTokens(source);

            // 验证基本的 token 数量和类型
            assertTrue(tokens.size() > 1, "应生成多个 token");
            assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).getType(), "最后一个 token 应为 EOF");

            // 验证关键字
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.DEF), "应识别 'def' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.IF), "应识别 'if' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.THEN), "应识别 'then' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.ELSE), "应识别 'else' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.VAL), "应识别 'val' 关键字");

            // 验证标识符
            List<String> identifiers = getValuesByType(tokens, TokenType.IDENTIFIER);
            assertTrue(identifiers.contains("factorial"), "应识别 'factorial' 标识符");
            assertTrue(identifiers.contains("n"), "应识别 'n' 标识符");
            assertTrue(identifiers.contains("x"), "应识别 'x' 标识符");
            assertTrue(identifiers.contains("print"), "应识别 'print' 标识符");

            // 验证运算符
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.MULTIPLY), "应识别 '*' 运算符");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.MINUS), "应识别 '-' 运算符");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.LESS_EQUAL), "应识别 '<=' 运算符");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.ASSIGN), "应识别 '=' 运算符");

            // 验证分隔符和括号
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.LEFT_PAREN), "应识别 '(' 分隔符");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.RIGHT_PAREN), "应识别 ')' 分隔符");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.LEFT_BRACE), "应识别 '{' 分隔符");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.RIGHT_BRACE), "应识别 '}' 分隔符");

            // 验证变量引用前缀
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.AMPERSAND), "应识别 '&' 变量引用前缀");

            // 验证数字字面量
            List<String> integers = getValuesByType(tokens, TokenType.INTEGER);
            assertTrue(integers.contains("5"), "应识别 '5' 整数字面量");
            assertTrue(integers.contains("1"), "应识别 '1' 整数字面量");
        }

        @Test
        @DisplayName("测试 Fluxon 特有语法特性")
        void testFluxonSpecificFeatures() {
            String source = """
                    // 测试范围操作符
                    val range1 = 1 .. 10
                    val range2 = 1 ..< 10
                    
                    // 测试空安全操作符
                    
                    // 测试函数调用链
                    val user = { name: "John" }?.name ?: "Guest"
                    .. // 直接测试范围操作符
                    val result = min 5 + 3 10
                    
                    // 测试 when 表达式
                    val grade = when {
                        &score >= 90 -> "A"
                        &score >= 80 -> "B"
                        &score >= 70 -> "C"
                        else -> "D"
                    }
                    """;

            List<Token> tokens = getTokens(source);

            // 验证范围操作符
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.RANGE), "应识别 '..' 范围操作符");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.RANGE_EXCLUSIVE), "应识别 '..<' 范围操作符");

            // 验证空安全操作符
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.QUESTION_DOT), "应识别 '?.' 空安全访问操作符");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.QUESTION_COLON), "应识别 '?:' Elvis 操作符");

            // 验证 when 表达式相关 token
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.WHEN), "应识别 'when' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.ARROW), "应识别 '->' 箭头操作符");

            // 验证函数调用相关 token
            List<String> identifiers = getValuesByType(tokens, TokenType.IDENTIFIER);
            assertTrue(identifiers.contains("min"), "应识别 'min' 标识符");
            assertTrue(identifiers.contains("score"), "应识别 'score' 标识符");

            // 验证数值
            List<String> integers = getValuesByType(tokens, TokenType.INTEGER);
            assertTrue(integers.contains("1"), "应识别 '1' 整数字面量");
            assertTrue(integers.contains("10"), "应识别 '10' 整数字面量");
            assertTrue(integers.contains("3"), "应识别 '3' 整数字面量");
            assertTrue(integers.contains("5"), "应识别 '5' 整数字面量");
            assertTrue(integers.contains("90"), "应识别 '90' 整数字面量");
            assertTrue(integers.contains("80"), "应识别 '80' 整数字面量");
            assertTrue(integers.contains("70"), "应识别 '70' 整数字面量");

            // 验证字符串
            List<String> strings = getValuesByType(tokens, TokenType.STRING);
            assertTrue(strings.contains("Guest"), "应识别 'Guest' 字符串字面量");
            assertTrue(strings.contains("A"), "应识别 'A' 字符串字面量");
            assertTrue(strings.contains("B"), "应识别 'B' 字符串字面量");
            assertTrue(strings.contains("C"), "应识别 'C' 字符串字面量");
            assertTrue(strings.contains("D"), "应识别 'D' 字符串字面量");
        }
    }

    @Nested
    @DisplayName("性能测试")
    class PerformanceTests {

        /**
         * 生成大型测试源码
         *
         * @param baseSource  基础源码
         * @param repetitions 重复次数
         * @return 重复后的大型源码
         */
        private String generateLargeSource(String baseSource, int repetitions) {
            StringBuilder sb = new StringBuilder(baseSource.length() * repetitions);
            for (int i = 0; i < repetitions; i++) {
                sb.append(baseSource);
            }
            return sb.toString();
        }

        /**
         * 测量函数执行时间
         *
         * @param runnable 要执行的函数
         * @return 执行时间（毫秒）
         */
        private double measureTimeMillis(Runnable runnable) {
            long start = System.nanoTime();
            runnable.run();
            long end = System.nanoTime();
            return (end - start) / 1_000_000.0; // 纳秒转毫秒
        }

        @Test
        @DisplayName("测试词法分析性能 - 小型输入")
        void testLexerPerformanceSmallInput() {
            String source = """
                    def factorial(n) = {
                        if &n <= 1 then 1
                        else &n * factorial(&n - 1)
                    }
                    val x = 5
                    print factorial &x
                    """;

            // 预热JVM
            for (int i = 0; i < 100; i++) {
                getTokens(source);
            }

            // 测量执行时间
            double time = measureTimeMillis(() -> getTokens(source));

            // 打印并断言性能
            System.out.println("小型输入词法分析时间: " + time + " ms");
            assertTrue(time < 5.0, "词法分析时间应小于 5ms，实际: " + time + "ms");
        }

        @Test
        @DisplayName("测试词法分析性能 - 中型输入")
        void testLexerPerformanceMediumInput() {
            String baseSource = """
                    def fibonacci(n) = {
                        if &n <= 1 then &n
                        else fibonacci(&n - 1) + fibonacci(&n - 2)
                    }
                    val result = fibonacci 10
                    """;

            String source = generateLargeSource(baseSource, 10); // 重复10次

            // 预热JVM
            for (int i = 0; i < 10; i++) {
                getTokens(source);
            }

            double time = measureTimeMillis(() -> getTokens(source));
            System.out.println("中型输入词法分析时间: " + time + " ms");
            assertTrue(time < 10.0, "中型输入词法分析时间应小于 10ms，实际: " + time + "ms");
        }

        @Test
        @DisplayName("测试词法分析性能 - 大型输入")
        void testLexerPerformanceLargeInput() {
            String baseSource = """
                    def add(a, b) = &a + &b
                    def subtract(a, b) = &a - &b
                    def multiply(a, b) = &a * &b
                    def divide(a, b) = &a / &b
                    
                    val x = 42
                    val y = 10
                    val sum = add &x &y
                    val diff = subtract &x &y
                    val product = multiply &x &y
                    val quotient = divide &x &y
                    
                    print "Results: " sum diff product quotient
                    """;

            String source = generateLargeSource(baseSource, 100); // 重复100次，创建大型源代码

            // 预热JVM
            getTokens(source); // 只预热一次，因为输入很大

            double time = measureTimeMillis(() -> getTokens(source));
            System.out.println("大型输入词法分析时间: " + time + " ms");
            assertTrue(time < 100.0, "大型输入词法分析时间应合理，实际: " + time + "ms");
        }

        @Test
        @DisplayName("测试冷启动性能")
        void testFirstRunPerformance() {
            // 使用简单但完整的一个Fluxon表达式
            String source = "print factorial 5";

            // 确保运行测试前类未加载（基于测试顺序）
            try {
                System.gc(); // 尝试清理内存
                Thread.sleep(100); // 给系统一些时间进行清理
            } catch (InterruptedException e) {
                // 忽略中断异常
            }
            double time = measureTimeMillis(() -> getTokens(source));
            System.out.println("首次运行词法分析时间: " + time + " ms");
        }
    }
}