package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonRuntimeTest;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.expression.ContextCall;
import org.tabooproject.fluxon.parser.expression.FunctionCall;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.expression.literal.StringLiteral;
import org.tabooproject.fluxon.parser.statement.Block;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 上下文调用（Context Call）语法测试
 */
public class ContextCallTest {

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
        CompilationContext context = new CompilationContext(source);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        Parser parser = new Parser();
        return parser.process(context);
    }

    /**
     * 执行源代码
     *
     * @param source 源代码
     * @return 执行结果
     */
    private Object executeSource(String source) {
        List<ParseResult> results = parseSource(source);
        Interpreter interpreter = new Interpreter();
        FluxonRuntime.getInstance().initializeEnvironment(interpreter.getEnvironment());
        
        Object result = null;
        for (ParseResult parseResult : results) {
            result = interpreter.evaluate(parseResult);
        }
        return result;
    }

    /**
     * 测试简单的上下文调用解析
     */
    @Test
    public void testSimpleContextCallParsing() {
        String source = "\"hello\" :: length";
        List<ParseResult> results = parseSource(source);
        
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ExpressionStatement);
        
        ExpressionStatement stmt = (ExpressionStatement) results.get(0);
        assertTrue(stmt.getExpression() instanceof ContextCall);
        
        ContextCall contextCall = (ContextCall) stmt.getExpression();
        assertTrue(contextCall.getTarget() instanceof StringLiteral);
        assertEquals("hello", ((StringLiteral) contextCall.getTarget()).getValue());
        
        assertTrue(contextCall.getContext() instanceof Identifier);
        assertEquals("length", ((Identifier) contextCall.getContext()).getValue());
    }

    /**
     * 测试上下文调用函数解析
     */
    @Test
    public void testContextCallFunctionParsing() {
        String source = "\"hello world\" :: replace(\"world\", \"fluxon\")";
        List<ParseResult> results = parseSource(source);
        
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ExpressionStatement);
        
        ExpressionStatement stmt = (ExpressionStatement) results.get(0);
        assertTrue(stmt.getExpression() instanceof ContextCall);
        
        ContextCall contextCall = (ContextCall) stmt.getExpression();
        assertTrue(contextCall.getTarget() instanceof StringLiteral);
        assertEquals("hello world", ((StringLiteral) contextCall.getTarget()).getValue());
        
        assertTrue(contextCall.getContext() instanceof FunctionCall);
        FunctionCall call = (FunctionCall) contextCall.getContext();
        assertTrue(call.getCallee() instanceof Identifier);
        assertEquals("replace", ((Identifier) call.getCallee()).getValue());
        assertEquals(2, call.getArguments().size());
    }

    /**
     * 测试上下文调用块表达式解析
     */
    @Test
    public void testContextCallBlockParsing() {
        String source = "\"hello\" :: { length }";
        List<ParseResult> results = parseSource(source);
        
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ExpressionStatement);
        
        ExpressionStatement stmt = (ExpressionStatement) results.get(0);
        assertTrue(stmt.getExpression() instanceof ContextCall);
        
        ContextCall contextCall = (ContextCall) stmt.getExpression();
        assertTrue(contextCall.getTarget() instanceof StringLiteral);
        assertEquals("hello", ((StringLiteral) contextCall.getTarget()).getValue());
        
        assertTrue(contextCall.getContext() instanceof Block);
        Block block = (Block) contextCall.getContext();
        assertEquals(1, block.getStatements().size());
    }

    /**
     * 测试链式上下文调用解析
     */
    @Test
    public void testChainedContextCallParsing() {
        String source = "\"hello\" :: length :: toString";
        List<ParseResult> results = parseSource(source);
        
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ExpressionStatement);
        
        ExpressionStatement stmt = (ExpressionStatement) results.get(0);
        assertTrue(stmt.getExpression() instanceof ContextCall);
        
        ContextCall outerCall = (ContextCall) stmt.getExpression();
        assertTrue(outerCall.getTarget() instanceof ContextCall);
        
        ContextCall innerCall = (ContextCall) outerCall.getTarget();
        assertTrue(innerCall.getTarget() instanceof StringLiteral);
        assertEquals("hello", ((StringLiteral) innerCall.getTarget()).getValue());
    }

    /**
     * 测试简单的上下文调用执行 - 属性访问
     */
    @Test
    public void testSimpleContextCallExecution() {
        Object result = executeSource("\"hello\" :: length");
        assertEquals(5, result);
    }

    /**
     * 测试上下文调用执行 - toString
     */
    @Test
    public void testContextCallToStringExecution() {
        Object result = executeSource("123 :: toString");
        assertEquals("123", result);
    }

    /**
     * 测试块表达式中的上下文调用
     */
    @Test
    public void testContextCallBlockExecution() {
        Object result = executeSource("\"hello\" :: { length }");
        assertEquals(5, result);
    }

    /**
     * 测试链式上下文调用执行
     */
    @Test
    public void testChainedContextCallExecution() {
        Object result = executeSource("\"hello\" :: length :: toString");
        assertEquals("5", result);
    }

    /**
     * 测试复杂的块表达式上下文调用
     */
    @Test
    public void testComplexContextCallBlockExecution() {
        String source = "\"hello world\" :: { " +
                        "  len = length; " +
                        "  len * 2 " +
                        "}";
        Object result = executeSource(source);
        assertEquals(22, result); // "hello world".length() * 2 = 11 * 2 = 22
    }
} 