package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.type.TestRuntime;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.expression.AwaitExpression;
import org.tabooproject.fluxon.parser.expression.IfExpression;
import org.tabooproject.fluxon.parser.expression.WhenExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.expression.literal.IntLiteral;
import org.tabooproject.fluxon.parser.statement.ReturnStatement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 解析器测试类
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ParserTest {

    @BeforeEach
    public void BeforeEach() {
        TestRuntime.registerTestFunctions();
    }

    /**
     * 解析源代码
     *
     * @param source 源代码
     * @return 解析结果列表
     */
    private List<ParseResult> parseSource(String source) {
        // 创建编译上下文
        CompilationContext context = new CompilationContext(source);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        Parser parser = new Parser();

        // 获取运行时环境并注册函数信息到解析器
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        parser.defineUserFunction(env.getRootFunctions());
        parser.defineRootVariables(env.getRootVariables());

        return parser.process(context);
    }

    /**
     * 测试简单的函数定义
     */
    @Test
    public void testSimpleFunctionDefinition() {
        String source = "def factorial(n) = n";
        List<ParseResult> results = parseSource(source);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof FunctionDefinition);

        FunctionDefinition func = (FunctionDefinition) results.get(0);
        assertEquals("factorial", func.getName());
        assertEquals(1, func.getParameters().size());
        assertNotNull(func.getParameters().get("n"));
        assertTrue(func.getBody() instanceof Identifier);
        assertEquals("n", ((Identifier) func.getBody()).getValue());
    }

    /**
     * 测试递归函数定义
     */
    @Test
    public void testRecursiveFunctionDefinition() {
        String source = "def factorial(n) = if &n <= 1 then 1 else &n * factorial(&n - 1)";
        List<ParseResult> results = parseSource(source);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof FunctionDefinition);

        FunctionDefinition func = (FunctionDefinition) results.get(0);
        assertEquals("factorial", func.getName());
        assertEquals(1, func.getParameters().size());
        assertNotNull(func.getParameters().get("n"));
        assertTrue(func.getBody() instanceof IfExpression);
    }

    /**
     * 测试异步函数定义
     */
    @Test
    public void testAsyncFunctionDefinition() {
        String source = "async def loadUser(id) = await fetch(\"users/${id}\")";
        List<ParseResult> results = parseSource(source);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof FunctionDefinition);

        FunctionDefinition func = (FunctionDefinition) results.get(0);
        assertEquals("loadUser", func.getName());
        assertTrue(func.isAsync());
        assertEquals(1, func.getParameters().size());
        assertNotNull(func.getParameters().get("id"));
        assertTrue(func.getBody() instanceof AwaitExpression);
    }

    /**
     * 测试 when 表达式
     */
    @Test
    public void testWhenExpression() {
        String source = "def describe(num) = when { &num % 2 == 0 -> \"even\"; &num < 0 -> \"negative(odd)\"; else -> \"positive(odd)\" }";
        List<ParseResult> results = parseSource(source);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof FunctionDefinition);

        FunctionDefinition func = (FunctionDefinition) results.get(0);
        assertEquals("describe", func.getName());
        assertEquals(1, func.getParameters().size());
        assertNotNull(func.getParameters().get("num"));
        assertTrue(func.getBody() instanceof WhenExpression);

        WhenExpression when = (WhenExpression) func.getBody();
        assertEquals(3, when.getBranches().size());
    }

    /**
     * 测试带有返回值的return语句
     */
    @Test
    public void testReturnStatementWithValue() {
        String source = "return 42";
        List<ParseResult> results = parseSource(source);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ReturnStatement);

        ReturnStatement returnStmt = (ReturnStatement) results.get(0);
        assertNotNull(returnStmt.getValue());
        assertTrue(returnStmt.getValue() instanceof IntLiteral);
        assertEquals(42, ((IntLiteral) returnStmt.getValue()).getValue());
    }

    /**
     * 测试不带返回值的return语句
     */
    @Test
    public void testReturnStatementWithoutValue() {
        String source = "return";
        List<ParseResult> results = parseSource(source);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ReturnStatement);

        ReturnStatement returnStmt = (ReturnStatement) results.get(0);
        assertNull(returnStmt.getValue());
    }

    @Test
    public void testIdentifier() {
        List<ParseResult> results = parseSource("test");
        System.out.println(results);
    }
}