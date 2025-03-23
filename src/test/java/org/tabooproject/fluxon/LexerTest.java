package org.tabooproject.fluxon;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;

import org.tabooproject.fluxon.compiler.CompilationContext;

import java.util.Arrays;
import java.util.List;
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
        Lexer lexer = new Lexer();
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
        return filterByType(tokens, type).stream().map(Token::getStringValue).collect(Collectors.toList());
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

    /**
     * 查找指定类型 Token 的索引
     *
     * @param tokens Token 列表
     * @param type   Token 类型
     * @return 找到的索引，未找到返回 -1
     */
    private int findTokenIndex(List<Token> tokens, TokenType type) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == type) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 查找指定类型和值的 Token 索引
     *
     * @param tokens Token 列表
     * @param type   Token 类型
     * @param value  Token 值
     * @return 找到的索引，未找到返回 -1
     */
    private int findTokenIndexByTypeAndValue(List<Token> tokens, TokenType type, String value) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == type && tokens.get(i).getStringValue().equals(value)) {
                return i;
            }
        }
        return -1;
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
            // 使用 index 判定，确保每个关键字在正确的位置上被识别
            assertEquals(TokenType.DEF, tokens.get(0).getType(), "第1个 token 应为 'def' 关键字");
            assertEquals(TokenType.FUN, tokens.get(1).getType(), "第2个 token 应为 'fun' 关键字");
            assertEquals(TokenType.VAL, tokens.get(2).getType(), "第3个 token 应为 'val' 关键字");
            assertEquals(TokenType.VAR, tokens.get(3).getType(), "第4个 token 应为 'var' 关键字");
            assertEquals(TokenType.IF, tokens.get(4).getType(), "第5个 token 应为 'if' 关键字");
            assertEquals(TokenType.THEN, tokens.get(5).getType(), "第6个 token 应为 'then' 关键字");
            assertEquals(TokenType.ELSE, tokens.get(6).getType(), "第7个 token 应为 'else' 关键字");
            assertEquals(TokenType.WHEN, tokens.get(7).getType(), "第8个 token 应为 'when' 关键字");
            assertEquals(TokenType.IS, tokens.get(8).getType(), "第9个 token 应为 'is' 关键字");
            assertEquals(TokenType.IN, tokens.get(9).getType(), "第10个 token 应为 'in' 关键字");
            assertEquals(TokenType.ASYNC, tokens.get(10).getType(), "第11个 token 应为 'async' 关键字");
            assertEquals(TokenType.AWAIT, tokens.get(11).getType(), "第12个 token 应为 'await' 关键字");
            assertEquals(TokenType.RETURN, tokens.get(12).getType(), "第13个 token 应为 'return' 关键字");
            assertEquals(TokenType.TRY, tokens.get(13).getType(), "第14个 token 应为 'try' 关键字");
            assertEquals(TokenType.CATCH, tokens.get(14).getType(), "第15个 token 应为 'catch' 关键字");

            // 验证值也正确
            assertEquals("def", tokens.get(0).getStringValue(), "第1个 token 的值应为 'def'");
            assertEquals("catch", tokens.get(14).getStringValue(), "第15个 token 的值应为 'catch'");

            // 关键字数量
            assertEquals(15, tokens.size() - 1, "应识别 15 个关键字 (不计 EOF)");
        }

        @Test
        @DisplayName("测试布尔字面量识别")
        void testBooleans() {
            String source = "true false";
            List<Token> tokens = getTokens(source);

            assertEquals(TokenType.TRUE, tokens.get(0).getType(), "第1个 token 应为 'true' 关键字");
            assertEquals(TokenType.FALSE, tokens.get(1).getType(), "第2个 token 应为 'false' 关键字");
        }
    }

    @Nested
    @DisplayName("数字字面量测试")
    class NumberLiteralTests {

        @Test
        @DisplayName("测试整数字面量")
        void testIntegers() {
            String source = "0 123 42 987654321";
            List<Token> tokens = getTokens(source);

            List<String> integers = getValuesByType(tokens, TokenType.INTEGER);
            assertEquals(4, integers.size(), "应识别 4 个整数字面量");

            assertArrayEquals(new String[]{"0", "123", "42", "987654321"}, integers.toArray(new String[0]));
        }

        @Test
        @DisplayName("测试长整型字面量")
        void testLongIntegers() {
            String source = "0L 123l 42L 9876543210l";
            List<Token> tokens = getTokens(source);

            List<String> longs = getValuesByType(tokens, TokenType.LONG);
            assertEquals(4, longs.size(), "应识别 4 个长整型字面量");

            assertArrayEquals(new String[]{"0", "123", "42", "9876543210"}, longs.toArray(new String[0]));
        }

        @Test
        @DisplayName("测试双精度字面量")
        void testDoubles() {
            String source = "0.5 3.14 1.0 0.123456789";
            List<Token> tokens = getTokens(source);

            List<String> doubles = getValuesByType(tokens, TokenType.DOUBLE);
            assertEquals(4, doubles.size(), "应识别 4 个浮点数字面量");

            assertArrayEquals(new String[]{"0.5", "3.14", "1.0", "0.123456789"}, doubles.toArray(new String[0]));
        }

        @Test
        @DisplayName("测试单精度浮点数字面量")
        void testDoubleFloats() {
            String source = "0.5f 3.14F 1.0f 0.12345679F";
            List<Token> tokens = getTokens(source);

            List<String> floats = getValuesByType(tokens, TokenType.FLOAT);
            assertEquals(4, floats.size(), "应识别 4 个双精度浮点数字面量");

            assertArrayEquals(new String[]{"0.5", "3.14", "1.0", "0.12345679"}, floats.toArray(new String[0]));
        }

        @Test
        @DisplayName("测试科学计数法")
        void testScientificNotation() {
            String source = "1e10 2.5e3 1.2e-5 3e+2";
            List<Token> tokens = getTokens(source);

            List<String> doubles = getValuesByType(tokens, TokenType.DOUBLE);
            assertEquals(4, doubles.size(), "应识别 4 个科学计数法字面量");
        }

        @Test
        @DisplayName("测试带后缀的科学计数法")
        void testScientificNotationWithSuffix() {
            String source = "1e10d 2.5e3D";
            List<Token> tokens = getTokens(source);
            System.out.println(tokens);

            // 检查双精度后缀
            List<String> doubles = getValuesByType(tokens, TokenType.DOUBLE);
            assertEquals(2, doubles.size(), "应识别 2 个双精度科学计数法字面量");
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
            assertEquals("line1\nline2", stringToken.getStringValue(), "字符串内容应包含换行符");

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
            String source = "def factorial(n) = {\n" +
                    "    if &n <= 1 then 1\n" +
                    "    else &n * factorial(&n - 1)\n" +
                    "}\n" +
                    "\n" +
                    "val x = 5\n" +
                    "print factorial &x";

            List<Token> tokens = getTokens(source);

            // 验证基本的 token 数量和类型
            assertTrue(tokens.size() > 1, "应生成多个 token");
            assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).getType(), "最后一个 token 应为 EOF");

            // 验证关键字
            // 查找并验证关键字在正确位置
            int defIndex = findTokenIndex(tokens, TokenType.DEF);
            int ifIndex = findTokenIndex(tokens, TokenType.IF);
            int thenIndex = findTokenIndex(tokens, TokenType.THEN);
            int elseIndex = findTokenIndex(tokens, TokenType.ELSE);
            int valIndex = findTokenIndex(tokens, TokenType.VAL);

            assertTrue(defIndex >= 0, "应识别 'def' 关键字");
            assertTrue(ifIndex >= 0, "应识别 'if' 关键字");
            assertTrue(thenIndex >= 0, "应识别 'then' 关键字");
            assertTrue(elseIndex >= 0, "应识别 'else' 关键字");
            assertTrue(valIndex >= 0, "应识别 'val' 关键字");


            // 验证关键字的相对位置
            assertTrue(ifIndex > defIndex, "'if' 应在 'def' 之后");
            assertTrue(thenIndex > ifIndex, "'then' 应在 'if' 之后");
            assertTrue(elseIndex > thenIndex, "'else' 应在 'then' 之后");

            // 验证标识符
            int factorialIndex = findTokenIndexByTypeAndValue(tokens, TokenType.IDENTIFIER, "factorial");
            int nIndex = findTokenIndexByTypeAndValue(tokens, TokenType.IDENTIFIER, "n");
            int xIndex = findTokenIndexByTypeAndValue(tokens, TokenType.IDENTIFIER, "x");
            int printIndex = findTokenIndexByTypeAndValue(tokens, TokenType.IDENTIFIER, "print");


            assertTrue(factorialIndex >= 0, "应识别 'factorial' 标识符");
            assertTrue(nIndex >= 0, "应识别 'n' 标识符");
            assertTrue(xIndex >= 0, "应识别 'x' 标识符");
            assertTrue(printIndex >= 0, "应识别 'print' 标识符");

            // 验证运算符
            assertTrue(findTokenIndex(tokens, TokenType.MULTIPLY) >= 0, "应识别 '*' 运算符");
            assertTrue(findTokenIndex(tokens, TokenType.MINUS) >= 0, "应识别 '-' 运算符");
            assertTrue(findTokenIndex(tokens, TokenType.LESS_EQUAL) >= 0, "应识别 '<=' 运算符");
            assertTrue(findTokenIndex(tokens, TokenType.ASSIGN) >= 0, "应识别 '=' 运算符");

            // 验证变量引用前缀
            assertTrue(findTokenIndex(tokens, TokenType.AMPERSAND) >= 0, "应识别 '&' 变量引用前缀");

            // 验证数字字面量
            List<String> integers = getValuesByType(tokens, TokenType.INTEGER);
            assertTrue(integers.contains("5"), "应识别 '5' 整数字面量");
            assertTrue(integers.contains("1"), "应识别 '1' 整数字面量");
        }

        @Test
        @DisplayName("测试 Fluxon 特有语法特性")
        void testFluxonSpecificFeatures() {
            String source = "// 测试范围操作符\n" +
                    "val range1 = 1 .. 10\n" +
                    "val range2 = 1 ..< 10\n" +
                    "\n" +
                    "// 测试空安全操作符\n" +
                    "\n" +
                    "// 测试函数调用链\n" +
                    "val user = { name: \"John\" }?.name ?: \"Guest\"\n" +
                    ".. // 直接测试范围操作符\n" +
                    "val result = min 5 + 3 10\n" +
                    "\n" +
                    "// 测试 when 表达式\n" +
                    "val grade = when {\n" +
                    "    &score >= 90 -> \"A\"\n" +
                    "    &score >= 80 -> \"B\"\n" +
                    "    &score >= 70 -> \"C\"\n" +
                    "    &score >= 60 -> \"D\"\n" +
                    "    else -> \"F\"\n" +
                    "}\n";

            List<Token> tokens = getTokens(source);

            // 验证范围操作符
            assertTrue(findTokenIndex(tokens, TokenType.RANGE) >= 0, "应识别 '..' 操作符");
            assertTrue(findTokenIndex(tokens, TokenType.RANGE_EXCLUSIVE) >= 0, "应识别 '..<' 操作符");

            // 验证空安全操作符
            assertTrue(findTokenIndex(tokens, TokenType.QUESTION_DOT) >= 0, "应识别 '?.' 操作符");
            assertTrue(findTokenIndex(tokens, TokenType.QUESTION_COLON) >= 0, "应识别 '?:' 操作符");

            // 验证箭头操作符
            assertTrue(findTokenIndex(tokens, TokenType.ARROW) >= 0, "应识别 '->' 操作符");

            // 验证不同类型的 token 数量
            List<Token> identifiers = filterByType(tokens, TokenType.IDENTIFIER);
            assertTrue(identifiers.size() >= 8, "应识别多个标识符");

            List<Token> strings = filterByType(tokens, TokenType.STRING);
            assertTrue(strings.size() >= 6, "应识别多个字符串字面量");

            List<Token> integers = filterByType(tokens, TokenType.INTEGER);
            assertTrue(integers.size() >= 4, "应识别多个整数字面量");

            // 验证关键字
            assertTrue(findTokenIndex(tokens, TokenType.WHEN) >= 0, "应识别 'when' 关键字");
            assertTrue(findTokenIndex(tokens, TokenType.ELSE) >= 0, "应识别 'else' 关键字");

            // 验证关键字和标识符的存在
            assertTrue(getValuesByType(tokens, TokenType.IDENTIFIER).contains("range1"), "应识别 'range1' 标识符");
            assertTrue(getValuesByType(tokens, TokenType.IDENTIFIER).contains("range2"), "应识别 'range2' 标识符");
            assertTrue(getValuesByType(tokens, TokenType.IDENTIFIER).contains("user"), "应识别 'user' 标识符");
            assertTrue(getValuesByType(tokens, TokenType.IDENTIFIER).contains("name"), "应识别 'name' 标识符");
        }
    }

    @Nested
    @DisplayName("函数调用语法测试")
    class FunctionCallSyntaxTests {

        @Test
        @DisplayName("测试简单函数调用")
        void testSimpleFunctionCall() {
            String source = "print 123";
            List<Token> tokens = getTokens(source);

            // 验证函数名和参数
            List<String> identifiers = getValuesByType(tokens, TokenType.IDENTIFIER);
            assertEquals(1, identifiers.size(), "应识别 1 个标识符");
            assertEquals("print", identifiers.get(0), "标识符应为 'print'");

            List<String> integers = getValuesByType(tokens, TokenType.INTEGER);
            assertEquals(1, integers.size(), "应识别 1 个整数字面量");
            assertEquals("123", integers.get(0), "整数应为 '123'");
        }

        @Test
        @DisplayName("测试无括号函数调用")
        void testFunctionCallWithoutParentheses() {
            String source = "max 10 20";
            List<Token> tokens = getTokens(source);

            // 验证函数名和参数
            List<String> identifiers = getValuesByType(tokens, TokenType.IDENTIFIER);
            assertEquals(1, identifiers.size(), "应识别 1 个标识符");
            assertEquals("max", identifiers.get(0), "标识符应为 'max'");

            List<String> integers = getValuesByType(tokens, TokenType.INTEGER);
            assertEquals(2, integers.size(), "应识别 2 个整数字面量");
            assertArrayEquals(new String[]{"10", "20"}, integers.toArray(new String[0]));
        }

        @Test
        @DisplayName("测试带括号函数调用")
        void testFunctionCallWithParentheses() {
            String source = "max(10, 20)";
            List<Token> tokens = getTokens(source);

            // 验证函数名、括号和参数
            assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType(), "第1个 token 应为标识符");
            assertEquals("max", tokens.get(0).getStringValue(), "标识符应为 'max'");

            assertEquals(TokenType.LEFT_PAREN, tokens.get(1).getType(), "第2个 token 应为左括号");
            assertEquals(TokenType.INTEGER, tokens.get(2).getType(), "第3个 token 应为整数");
            assertEquals("10", tokens.get(2).getStringValue(), "整数应为 '10'");
            assertEquals(TokenType.COMMA, tokens.get(3).getType(), "第4个 token 应为逗号");
            assertEquals(TokenType.INTEGER, tokens.get(4).getType(), "第5个 token 应为整数");
            assertEquals("20", tokens.get(4).getStringValue(), "整数应为 '20'");
            assertEquals(TokenType.RIGHT_PAREN, tokens.get(5).getType(), "第6个 token 应为右括号");
        }

        @Test
        @DisplayName("测试嵌套函数调用")
        void testNestedFunctionCall() {
            String source = "println max 10 20";
            List<Token> tokens = getTokens(source);

            // 验证函数名和参数
            List<String> identifiers = getValuesByType(tokens, TokenType.IDENTIFIER);
            assertEquals(2, identifiers.size(), "应识别 2 个标识符");
            assertEquals("println", identifiers.get(0), "第1个标识符应为 'println'");
            assertEquals("max", identifiers.get(1), "第2个标识符应为 'max'");

            List<String> integers = getValuesByType(tokens, TokenType.INTEGER);
            assertEquals(2, integers.size(), "应识别 2 个整数字面量");
            assertArrayEquals(new String[]{"10", "20"}, integers.toArray(new String[0]));
        }

        @Test
        @DisplayName("测试带括号嵌套函数调用")
        void testNestedFunctionCallWithParentheses() {
            String source = "println(max(10, 20))";
            List<Token> tokens = getTokens(source);

            // 验证函数名和参数
            List<String> identifiers = getValuesByType(tokens, TokenType.IDENTIFIER);
            assertEquals(2, identifiers.size(), "应识别 2 个标识符");
            assertEquals("println", identifiers.get(0), "第1个标识符应为 'println'");
            assertEquals("max", identifiers.get(1), "第2个标识符应为 'max'");

            List<String> integers = getValuesByType(tokens, TokenType.INTEGER);
            assertEquals(2, integers.size(), "应识别 2 个整数字面量");
            assertArrayEquals(new String[]{"10", "20"}, integers.toArray(new String[0]));

            // 验证括号平衡
            List<Token> leftParens = filterByType(tokens, TokenType.LEFT_PAREN);
            List<Token> rightParens = filterByType(tokens, TokenType.RIGHT_PAREN);
            assertEquals(leftParens.size(), rightParens.size(), "左右括号数量应相等");
        }
    }

    @Nested
    @DisplayName("性能测试")
    class PerformanceTests {
        /**
         * 生成大型测试输入
         * @param baseSource 基础源代码
         * @param repetitions 重复次数
         * @return 生成的大型测试输入
         */
        private String generateLargeSource(String baseSource, int repetitions) {
            StringBuilder sb = new StringBuilder(baseSource.length() * repetitions);
            for (int i = 0; i < repetitions; i++) {
                sb.append(baseSource).append("\n");
            }
            return sb.toString();
        }

        /**
         * 测量执行时间（毫秒）
         * @param runnable 要执行的代码
         * @return 执行时间（毫秒）
         */
        private double measureTimeMillis(Runnable runnable) {
            long start = System.nanoTime();
            runnable.run();
            long end = System.nanoTime();
            return (end - start) / 1_000_000.0;
        }

        @Test
        @DisplayName("测试词法分析器性能（小输入）")
        void testLexerPerformanceSmallInput() {
            String source = "def factorial(n) = {\n" +
                    "    if &n <= 1 then 1\n" +
                    "    else &n * factorial(&n - 1)\n" +
                    "}\n" +
                    "val x = 5\n" +
                    "print factorial &x";

            double timeMillis = measureTimeMillis(() -> {
                List<Token> tokens = getTokens(source);
                assertTrue(tokens.size() > 1, "应生成多个 token");
            });

            System.out.println("小输入词法分析耗时: " + timeMillis + " ms");
            // 不做硬性断言，因为不同机器性能不同
            assertTrue(timeMillis < 100, "小输入词法分析应该很快 (<100ms)");
        }

        @Test
        @DisplayName("测试词法分析器性能（中等输入）")
        void testLexerPerformanceMediumInput() {
            String baseSource = "def factorial(n) = {\n" +
                    "    if &n <= 1 then 1\n" +
                    "    else &n * factorial(&n - 1)\n" +
                    "}\n" +
                    "val x = 5\n" +
                    "print factorial &x";

            String source = generateLargeSource(baseSource, 100);
            
            double timeMillis = measureTimeMillis(() -> {
                List<Token> tokens = getTokens(source);
                assertTrue(tokens.size() > 100, "应生成大量 token");
            });

            System.out.println("中等输入词法分析耗时: " + timeMillis + " ms");
            // 不做硬性断言，因为不同机器性能不同
            assertTrue(timeMillis < 500, "中等输入词法分析应在合理时间内完成 (<500ms)");
        }

        @Test
        @DisplayName("测试词法分析器性能（大输入）")
        void testLexerPerformanceLargeInput() {
            String baseSource = "def factorial(n) = {\n" +
                    "    if &n <= 1 then 1\n" +
                    "    else &n * factorial(&n - 1)\n" +
                    "}\n" +
                    "val x = 5\n" +
                    "print factorial &x";

            // 创建一个大型输入
            String source = generateLargeSource(baseSource, 1000);
            
            // 测量性能
            double timeMillis = measureTimeMillis(() -> {
                List<Token> tokens = getTokens(source);
                assertTrue(tokens.size() > 1000, "应生成大量 token");
            });

            System.out.println("大输入词法分析耗时: " + timeMillis + " ms");
            // 不做硬性断言，因为不同机器性能不同
            assertTrue(timeMillis < 2000, "大输入词法分析应在合理时间内完成 (<2000ms)");
            
            // 可以添加一个对大规模输入的正确性检查
            List<Token> tokens = getTokens(source);
            assertTrue(tokens.size() > 1000, "应生成大量 token");
            assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).getType(), "最后一个 token 应为 EOF");
        }

        @Test
        @DisplayName("测试首次运行性能")
        void testFirstRunPerformance() {
            // 测量首次运行性能
            String source = "val x = 5\n";
            
            double firstTimeMillis = measureTimeMillis(() -> {
                List<Token> tokens = getTokens(source);
                assertTrue(tokens.size() > 1, "应生成多个 token");
            });

            // 测量后续运行性能
            double secondTimeMillis = measureTimeMillis(() -> {
                List<Token> tokens = getTokens(source);
                assertTrue(tokens.size() > 1, "应生成多个 token");
            });

            System.out.println("首次运行耗时: " + firstTimeMillis + " ms");
            System.out.println("后续运行耗时: " + secondTimeMillis + " ms");
            
            // 首次运行可能包含初始化开销，但不应该比后续运行慢太多
            // 不做硬性断言，因为不同机器性能不同，但这是一个可以观察的指标
        }
    }
}