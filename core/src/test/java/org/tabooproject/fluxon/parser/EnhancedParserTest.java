package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonRuntimeTest;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.definition.Definitions.FunctionDefinition;
import org.tabooproject.fluxon.parser.expression.*;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.expression.literal.IntLiteral;
import org.tabooproject.fluxon.parser.expression.literal.StringLiteral;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 增强版解析器测试，覆盖更多边缘情况和复杂场景
 */
public class EnhancedParserTest {

    @BeforeAll
    public static void beforeAll() {
        FluxonRuntimeTest.registerTestFunctions();
        CompilationContext.DEFAULT_ALLOW_KETHER_STYLE_CALL = true;
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

        // 定义测试用的变量
        parser.defineVariable("variable");
        parser.defineVariable("x");
        parser.defineVariable("y");
        parser.defineVariable("value");

        return parser.process(context);
    }

    /**
     * 测试无括号函数调用的各种情况
     */
    @Test
    public void testNoBracketFunctionCalls() {
        // 基本无括号调用
        List<ParseResult> results = parseSource("print hello");
        assertEquals(1, results.size());
        ExpressionStatement stmt = (ExpressionStatement) results.get(0);
        FunctionCallExpression call = (FunctionCallExpression) stmt.getExpression();
        assertEquals("print", call.getCallee());
        assertEquals(1, call.getArguments().size());
        assertTrue(call.getArguments().get(0) instanceof StringLiteral);
        assertEquals("hello", ((StringLiteral) call.getArguments().get(0)).getValue());

        // 嵌套无括号调用
        results = parseSource("print checkGrade 95");
        assertEquals(1, results.size());
        stmt = (ExpressionStatement) results.get(0);
        call = (FunctionCallExpression) stmt.getExpression();
        assertEquals("print", call.getCallee());
        assertEquals(1, call.getArguments().size());
        assertTrue(call.getArguments().get(0) instanceof FunctionCallExpression);
        FunctionCallExpression nestedCall = (FunctionCallExpression) call.getArguments().get(0);
        assertEquals("checkGrade", nestedCall.getCallee());
        assertEquals(1, nestedCall.getArguments().size());
        assertEquals(95, ((IntLiteral) nestedCall.getArguments().get(0)).getValue());
    }

    /**
     * 测试未加引号的标识符自动转为字符串的情况
     */
    @Test
    public void testUnquotedIdentifiersAsStrings() {
        // 未知标识符作为字符串参数
        List<ParseResult> results = parseSource("player head");
        assertEquals(1, results.size());
        ExpressionStatement stmt = (ExpressionStatement) results.get(0);
        FunctionCallExpression call = (FunctionCallExpression) stmt.getExpression();
        assertEquals("player", call.getCallee());
        assertEquals(1, call.getArguments().size());
        assertTrue(call.getArguments().get(0) instanceof StringLiteral);
        assertEquals("head", ((StringLiteral) call.getArguments().get(0)).getValue());

        // 混合已知函数和未知标识符
        results = parseSource("player checkGrade 95");
        assertEquals(1, results.size());
        stmt = (ExpressionStatement) results.get(0);
        call = (FunctionCallExpression) stmt.getExpression();
        assertEquals(1, call.getArguments().size());
        assertTrue(call.getArguments().get(0) instanceof FunctionCallExpression);
        assertEquals("checkGrade", ((FunctionCallExpression) call.getArguments().get(0)).getCallee());

        // 多个未知标识符
        results = parseSource("player head body legs");
        System.out.println(results);
        assertEquals(1, results.size());
        stmt = (ExpressionStatement) results.get(0);
        call = (FunctionCallExpression) stmt.getExpression();
        assertEquals(3, call.getArguments().size());
        assertEquals("head", ((StringLiteral) call.getArguments().get(0)).getValue());
        assertEquals("body", ((StringLiteral) call.getArguments().get(1)).getValue());
        assertEquals("legs", ((StringLiteral) call.getArguments().get(2)).getValue());
    }

    /**
     * 测试嵌套表达式和复杂语法结构
     */
    @Test
    public void testNestedExpressionsAndComplexStructures() {
        // 嵌套的if表达式
        List<ParseResult> results = parseSource("if if true then 1 else 0 then \"true\" else \"false\"");
        assertEquals(1, results.size());
        ExpressionStatement stmt = (ExpressionStatement) results.get(0);
        IfExpression ifExpr = (IfExpression) stmt.getExpression();
        assertTrue(ifExpr.getCondition() instanceof IfExpression);

        // 嵌套的when表达式
        results = parseSource("when when true { true -> 1 else -> 0 } { 1 -> \"one\" 0 -> \"zero\" else -> \"other\" }");
        assertEquals(1, results.size());
        stmt = (ExpressionStatement) results.get(0);
        WhenExpression whenExpr = (WhenExpression) stmt.getExpression();
        assertTrue(whenExpr.getSubject() instanceof WhenExpression);

        // 复杂的二元表达式
        results = parseSource("1 + 2 * 3 - 4 / 5 % 6");
        assertEquals(1, results.size());
        stmt = (ExpressionStatement) results.get(0);
        assertTrue(stmt.getExpression() instanceof BinaryExpression);
    }

    /**
     * 测试异步函数和await表达式
     */
    @Test
    public void testAsyncFunctionsAndAwaitExpressions() {
        // 异步函数定义
        List<ParseResult> results = parseSource("async def fetchData(url) = await fetch(url)");
        assertEquals(1, results.size());
        FunctionDefinition funcDef = (FunctionDefinition) results.get(0);
        assertTrue(funcDef.isAsync());
        assertEquals("fetchData", funcDef.getName());
        assertEquals(1, funcDef.getParameters().size());
        assertNotNull(funcDef.getParameters().get("url"));
        assertTrue(funcDef.getBody() instanceof AwaitExpression);

        // 嵌套的await表达式
        results = parseSource("async def processData() = await processResult(await fetchData(\"api/data\"))");
        assertEquals(1, results.size());
        funcDef = (FunctionDefinition) results.get(0);
        assertTrue(funcDef.getBody() instanceof AwaitExpression);
        AwaitExpression awaitExpr = (AwaitExpression) funcDef.getBody();
        FunctionCallExpression call = (FunctionCallExpression) awaitExpr.getExpression();
        assertTrue(call.getArguments().get(0) instanceof AwaitExpression);
    }

    /**
     * 测试when表达式的各种分支情况
     */
    @Test
    public void testWhenExpressionBranches() {
        // 基本when表达式
        List<ParseResult> results = parseSource("when { true -> 1 false -> 0 }");
        assertEquals(1, results.size());
        ExpressionStatement stmt = (ExpressionStatement) results.get(0);
        WhenExpression whenExpr = (WhenExpression) stmt.getExpression();
        assertNull(whenExpr.getSubject());
        assertEquals(2, whenExpr.getBranches().size());

        // 带条件的when表达式
        results = parseSource("when x { 1 -> \"one\" 2 -> \"two\" else -> \"other\" }");
        assertEquals(1, results.size());
        stmt = (ExpressionStatement) results.get(0);
        whenExpr = (WhenExpression) stmt.getExpression();
        assertTrue(whenExpr.getSubject() instanceof Identifier);
        assertEquals(3, whenExpr.getBranches().size());

        // 复杂条件的when分支
        results = parseSource("when { &x % 2 == 0 -> \"even\" &x < 0 -> \"negative\" else -> \"positive odd\" }");
        assertEquals(1, results.size());
        stmt = (ExpressionStatement) results.get(0);
        whenExpr = (WhenExpression) stmt.getExpression();
        assertEquals(3, whenExpr.getBranches().size());
        assertTrue(whenExpr.getBranches().get(0).getCondition() instanceof BinaryExpression);
        assertTrue(whenExpr.getBranches().get(1).getCondition() instanceof BinaryExpression);
        assertNull(whenExpr.getBranches().get(2).getCondition());
    }

    /**
     * 测试引用表达式(&)
     */
    @Test
    public void testReferenceExpressions() {
        // 基本引用表达式
        List<ParseResult> results = parseSource("&variable");
        assertEquals(1, results.size());
        ExpressionStatement stmt = (ExpressionStatement) results.get(0);
        ReferenceExpression refExpr = (ReferenceExpression) stmt.getExpression();
        assertTrue(refExpr.getIdentifier() instanceof Identifier);
        assertEquals("variable", ((Identifier) refExpr.getIdentifier()).getValue());

        // 引用表达式在二元操作中
        results = parseSource("&x + &y");
        assertEquals(1, results.size());
        stmt = (ExpressionStatement) results.get(0);
        BinaryExpression binExpr = (BinaryExpression) stmt.getExpression();
        assertTrue(binExpr.getLeft() instanceof ReferenceExpression);
        assertTrue(binExpr.getRight() instanceof ReferenceExpression);

        // 引用表达式在函数调用中
        results = parseSource("print(&value)");
        assertEquals(1, results.size());
        stmt = (ExpressionStatement) results.get(0);
        FunctionCallExpression call = (FunctionCallExpression) stmt.getExpression();
        assertEquals(1, call.getArguments().size());
        assertTrue(call.getArguments().get(0) instanceof ReferenceExpression);
    }

    /**
     * 测试递归函数定义
     */
    @Test
    public void testRecursiveFunctionDefinition() {
        // 递归阶乘函数
        List<ParseResult> results = parseSource(
                "def factorial(n) = if &n <= 1 then 1 else &n * factorial(&n - 1)");
        assertEquals(1, results.size());
        FunctionDefinition funcDef = (FunctionDefinition) results.get(0);
        assertEquals("factorial", funcDef.getName());
        assertEquals(1, funcDef.getParameters().size());
        assertTrue(funcDef.getBody() instanceof IfExpression);

        // 递归斐波那契函数
        results = parseSource(
                "def fibonacci(n) = if &n <= 1 then &n else fibonacci(&n - 1) + fibonacci(&n - 2)");
        assertEquals(1, results.size());
        funcDef = (FunctionDefinition) results.get(0);
        assertEquals("fibonacci", funcDef.getName());
        assertTrue(funcDef.getBody() instanceof IfExpression);
        IfExpression ifExpr = (IfExpression) funcDef.getBody();
        assertTrue(ifExpr.getElseBranch() instanceof BinaryExpression);
    }

    /**
     * 测试错误处理和边缘情况
     */
    @Test
    public void testErrorHandlingAndEdgeCases() {
        // 缺少右括号
        assertThrows(ParseException.class, () -> parseSource("print(1, 2"));

        // 无效的赋值目标
        assertThrows(ParseException.class, () -> parseSource("1 + 2 = 3"));

        // 缺少箭头操作符
        assertThrows(ParseException.class, () -> parseSource("when { true 1 }"));

        // 空的代码块
        List<ParseResult> results = parseSource("def emptyFunc() = {}");
        assertEquals(1, results.size());
    }
}