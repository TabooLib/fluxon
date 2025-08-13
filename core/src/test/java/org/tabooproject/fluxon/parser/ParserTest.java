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
import org.tabooproject.fluxon.parser.statement.ReturnStatement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 解析器测试类
 */
public class ParserTest {

    @BeforeAll
    public static void beforeAll() {
        FluxonRuntimeTest.registerTestFunctions();
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
        parser.defineFunction(env.getFunctions());
        parser.defineVariables(env.getVariables());
        parser.defineExtensionFunction(env.getExtensionFunctions());
        
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
        assertEquals("n", func.getParameters().get(0));
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
        assertEquals("n", func.getParameters().get(0));
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
        assertEquals("id", func.getParameters().get(0));
        assertTrue(func.getBody() instanceof AwaitExpression);
    }
    
    /**
     * 测试 when 表达式
     */
    @Test
    public void testWhenExpression() {
        String source = "def describe(num) = when { &num % 2 == 0 -> \"even\"; &num < 0 -> \"negative odd\"; else -> \"positive odd\" }";
        List<ParseResult> results = parseSource(source);
        
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof FunctionDefinition);
        
        FunctionDefinition func = (FunctionDefinition) results.get(0);
        assertEquals("describe", func.getName());
        assertEquals(1, func.getParameters().size());
        assertEquals("num", func.getParameters().get(0));
        assertTrue(func.getBody() instanceof WhenExpression);
        
        WhenExpression when = (WhenExpression) func.getBody();
        assertEquals(3, when.getBranches().size());
    }
    
    /**
     * 测试无括号函数调用
     */
    @Test
    public void testNoBracketFunctionCall() {
        String source = "print checkGrade 85";
        List<ParseResult> results = parseSource(source);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ExpressionStatement);
        
        ExpressionStatement stmt = (ExpressionStatement) results.get(0);
        assertTrue(stmt.getExpression() instanceof FunctionCall);
        
        FunctionCall call = (FunctionCall) stmt.getExpression();
        assertTrue(call.getCallee() instanceof Identifier);
        assertEquals("print", ((Identifier) call.getCallee()).getValue());
        assertEquals(1, call.getArguments().size());
        
        ParseResult arg = call.getArguments().get(0);
        assertTrue(arg instanceof FunctionCall);
        
        FunctionCall innerCall = (FunctionCall) arg;
        assertTrue(innerCall.getCallee() instanceof Identifier);
        assertEquals("checkGrade", ((Identifier) innerCall.getCallee()).getValue());
        assertEquals(1, innerCall.getArguments().size());
        assertTrue(innerCall.getArguments().get(0) instanceof IntLiteral);
        assertEquals(85, ((IntLiteral) innerCall.getArguments().get(0)).getValue());
    }
    
    /**
     * 测试未加引号标识符自动转为字符串
     */
    @Test
    public void testUnquotedIdentifierAsString() {
        String source = "player head";
        List<ParseResult> results = parseSource(source);
        
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ExpressionStatement);
        
        ExpressionStatement stmt = (ExpressionStatement) results.get(0);
        assertTrue(stmt.getExpression() instanceof FunctionCall);
        
        FunctionCall call = (FunctionCall) stmt.getExpression();
        assertTrue(call.getCallee() instanceof Identifier);
        assertEquals("player", ((Identifier) call.getCallee()).getValue());
        assertEquals(1, call.getArguments().size());
        assertTrue(call.getArguments().get(0) instanceof StringLiteral);
        assertEquals("head", ((StringLiteral) call.getArguments().get(0)).getValue());
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
    
}