package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.definitions.Definitions.FunctionDefinition;
import org.tabooproject.fluxon.parser.expressions.Expressions.*;
import org.tabooproject.fluxon.parser.statements.Statements.ExpressionStatement;
import org.tabooproject.fluxon.parser.statements.Statements.ReturnStatement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 解析器测试类
 */
public class ParserTest {

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
        assertTrue(func.getBody() instanceof Variable);
        assertEquals("n", ((Variable) func.getBody()).getName());
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
        assertTrue(call.getCallee() instanceof Variable);
        assertEquals("print", ((Variable) call.getCallee()).getName());
        assertEquals(1, call.getArguments().size());
        
        ParseResult arg = call.getArguments().get(0);
        assertTrue(arg instanceof FunctionCall);
        
        FunctionCall innerCall = (FunctionCall) arg;
        assertTrue(innerCall.getCallee() instanceof Variable);
        assertEquals("checkGrade", ((Variable) innerCall.getCallee()).getName());
        assertEquals(1, innerCall.getArguments().size());
        assertTrue(innerCall.getArguments().get(0) instanceof IntegerLiteral);
        assertEquals("85", ((IntegerLiteral) innerCall.getArguments().get(0)).getValue());
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
        assertTrue(call.getCallee() instanceof Variable);
        assertEquals("player", ((Variable) call.getCallee()).getName());
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
        assertTrue(returnStmt.getValue() instanceof IntegerLiteral);
        assertEquals("42", ((IntegerLiteral) returnStmt.getValue()).getValue());
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