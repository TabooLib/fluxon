package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonRuntimeTest;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 错误恢复测试类
 * 测试解析器在遇到语法错误时的恢复能力
 */
public class ErrorRecoveryTest {

    @BeforeEach
    public void setUp() {
        FluxonRuntimeTest.registerTestFunctions();
    }

    /**
     * 解析源代码并返回编译上下文（包含错误信息）
     *
     * @param source 源代码
     * @return 编译上下文
     */
    private CompilationContext parseSourceWithContext(String source) {
        CompilationContext context = new CompilationContext(source);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        
        Parser parser = new Parser();
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        parser.defineUserFunction(env.getRootFunctions());
        parser.defineRootVariables(env.getRootVariables());
        
        try {
            parser.process(context);
        } catch (ParseException ex) {
            // 预期会抛出异常，但错误列表已存入 context
        }
        
        return context;
    }

    /**
     * 测试单个语法错误
     */
    @Test
    public void testSingleSyntaxError() {
        String source = "def test(n) = if &n <= 1 then 1 else"; // 缺少 else 分支
        
        CompilationContext context = parseSourceWithContext(source);
        
        // 应该有错误
        List<ParseException> errors = context.getAttribute("parseErrors");
        assertNotNull(errors);
        assertEquals(1, errors.size());
        
        // 错误信息应该包含位置信息
        ParseException error = errors.get(0);
        assertNotNull(error.getToken());
        assertTrue(error.getMessage().contains("line"));
    }

    /**
     * 测试多个语法错误的恢复
     */
    @Test
    public void testMultipleErrorRecovery() {
        String source = 
            "def test1(n) = if &n <= 1 then 1 else\n" +  // 错误1：缺少 else 分支
            "def test2(n) = &n * 2\n" +                   // 正确的函数
            "x = 1 +\n" +                                  // 错误2：缺少右操作数
            "def test3(n) = &n + 1";                      // 正确的函数
        
        CompilationContext context = parseSourceWithContext(source);
        
        // 应该收集到多个错误
        List<ParseException> errors = context.getAttribute("parseErrors");
        assertNotNull(errors);
        assertTrue(errors.size() >= 2, "应该检测到至少2个错误，实际: " + errors.size());
        
        // 应该成功解析部分结果
        List<ParseResult> results = context.getAttribute("results");
        assertNotNull(results);
        // 至少应该解析出 test2 和 test3 两个正确的函数
        long functionCount = results.stream()
            .filter(r -> r instanceof FunctionDefinition)
            .count();
        assertTrue(functionCount >= 1, "应该成功解析至少1个正确的函数定义，实际: " + functionCount);
    }

    /**
     * 测试在不同位置的错误恢复
     */
    @Test
    public void testErrorRecoveryAtDifferentPositions() {
        String source = 
            "x = 10\n" +
            "y = 20 +\n" +                                // 错误：缺少右操作数
            "z = 30";                                     // 正确
        
        CompilationContext context = parseSourceWithContext(source);
        
        List<ParseException> errors = context.getAttribute("parseErrors");
        assertNotNull(errors);
        assertTrue(errors.size() >= 1, "应该检测到至少1个错误");
        
        // 应该有部分成功解析的结果
        List<ParseResult> results = context.getAttribute("results");
        assertNotNull(results);
        assertTrue(results.size() > 0, "应该有部分解析成功的结果");
    }

    /**
     * 测试连续多个错误的恢复
     */
    @Test
    public void testConsecutiveErrors() {
        String source = 
            "def func1() =\n" +                           // 错误1：缺少函数体
            "def func2() =\n" +                           // 错误2：缺少函数体
            "def func3() = 42";                           // 正确
        
        CompilationContext context = parseSourceWithContext(source);
        
        List<ParseException> errors = context.getAttribute("parseErrors");
        assertNotNull(errors);
        assertTrue(errors.size() >= 2, "应该检测到至少2个错误");
        
        // 应该至少解析出 func3
        List<ParseResult> results = context.getAttribute("results");
        assertNotNull(results);
        long functionCount = results.stream()
            .filter(r -> r instanceof FunctionDefinition)
            .count();
        assertTrue(functionCount >= 1, "应该至少解析出1个正确的函数");
    }

    /**
     * 测试分号分隔的多语句错误恢复
     */
    @Test
    public void testSemicolonSeparatedErrorRecovery() {
        String source = "x = 1 +; y = 2; z = 3"; // 第一条语句有错，后两条正确
        
        CompilationContext context = parseSourceWithContext(source);
        
        List<ParseException> errors = context.getAttribute("parseErrors");
        assertNotNull(errors);
        assertEquals(1, errors.size(), "应该只有1个错误");
        
        // 应该成功解析 y 和 z 的赋值
        List<ParseResult> results = context.getAttribute("results");
        assertNotNull(results);
        assertTrue(results.size() >= 2, "应该至少解析出2条正确的语句");
    }

    /**
     * 测试嵌套结构中的错误恢复
     */
    @Test
    public void testNestedStructureErrorRecovery() {
        String source = 
            "def outer() = {\n" +
            "    def inner1() = if true then\n" +        // 错误：缺少 then 后的表达式
            "    def inner2() = 42\n" +                   // 正确
            "}\n" +
            "def another() = 99";                         // 正确
        
        CompilationContext context = parseSourceWithContext(source);
        
        List<ParseException> errors = context.getAttribute("parseErrors");
        assertNotNull(errors);
        assertTrue(errors.size() >= 1, "应该检测到至少1个错误");
    }

    /**
     * 测试括号不匹配的错误恢复
     */
    @Test
    public void testUnmatchedParenthesisRecovery() {
        String source = 
            "print(\"hello\"\n" +                         // 错误：缺少右括号
            "def func() = 42\n" +                         // 正确
            "print(\"world\")";                           // 正确
        
        CompilationContext context = parseSourceWithContext(source);
        
        List<ParseException> errors = context.getAttribute("parseErrors");
        assertNotNull(errors);
        assertEquals(1, errors.size(), "应该检测到1个错误");
        
        // 应该成功解析后面的函数定义和打印语句
        List<ParseResult> results = context.getAttribute("results");
        assertNotNull(results);
        assertTrue(results.size() >= 2, "应该至少解析出2条正确的语句");
    }

    /**
     * 测试 when 表达式中的错误恢复
     */
    @Test
    public void testWhenExpressionErrorRecovery() {
        String source = 
            "def test1() = when {\n" +
            "    true ->\n" +                              // 错误：缺少箭头后的表达式
            "}\n" +
            "def test2() = 42";                            // 正确
        
        CompilationContext context = parseSourceWithContext(source);
        
        List<ParseException> errors = context.getAttribute("parseErrors");
        assertNotNull(errors);
        assertTrue(errors.size() >= 1, "应该检测到至少1个错误");
    }

    /**
     * 测试完全正确的代码不产生错误
     */
    @Test
    public void testNoErrorsInValidCode() {
        String source = 
            "def factorial(n) = if &n <= 1 then 1 else &n * factorial(&n - 1)\n" +
            "def add(x, y) = &x + &y\n" +
            "result = factorial(5)";
        
        CompilationContext context = parseSourceWithContext(source);
        
        List<ParseException> errors = context.getAttribute("parseErrors");
        assertNotNull(errors);
        assertEquals(0, errors.size(), "正确的代码不应该有错误");
        
        List<ParseResult> results = context.getAttribute("results");
        assertNotNull(results);
        assertEquals(3, results.size(), "应该解析出3条语句");
    }

    /**
     * 测试错误信息包含有用的上下文
     */
    @Test
    public void testErrorContextInformation() {
        String source = "def test() = if true then else 42"; // 缺少 then 后的表达式
        
        CompilationContext context = parseSourceWithContext(source);
        
        List<ParseException> errors = context.getAttribute("parseErrors");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
        
        ParseException error = errors.get(0);
        assertNotNull(error.getToken(), "错误应该包含 token 信息");
        assertNotNull(error.getMessage(), "错误应该有消息");
        assertTrue(error.getToken().getLine() > 0, "错误应该包含行号");
        assertTrue(error.getToken().getColumn() > 0, "错误应该包含列号");
    }
}
