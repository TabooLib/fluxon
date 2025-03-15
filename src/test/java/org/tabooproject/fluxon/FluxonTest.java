package org.tabooproject.fluxon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

import org.tabooproject.fluxon.ast.AstPrinter;
import org.tabooproject.fluxon.ast.expression.*;
import org.tabooproject.fluxon.ast.statement.*;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.Parser;

import java.util.List;

/**
 * Fluxon 测试类
 * 使用 JUnit 测试编译器的功能
 */
public class FluxonTest {

    private static final String MULTI_STMT_FUNCTION = """
            def test(n) = {
                val a = &n * 2
                val b = &a + 5
                &b
            }
            
            def factorial(n) = {
                if &n <= 1 then 1
                else &n * factorial(&n - 1)
                // 这里故意多一个表达式测试块表达式
                &n
            }
            """;

    private static final String FACTORIAL_PROGRAM = """
            // 这是一个简单的 Fluxon 程序
            
            // 定义 print 函数
            def print(value) = {
                &value
            }
            
            // 定义一个函数
            def factorial(n) = {
                if &n <= 1 then 1
                else &n * factorial(&n - 1)
            }
            
            // 变量声明
            val x = 5
            
            // 函数调用
            print factorial &x
            """;

    private CompilationContext context;
    private Lexer lexer;
    private List<Token> tokens;

    @BeforeEach
    void setUp() {
        context = new CompilationContext(FACTORIAL_PROGRAM);
        lexer = new Lexer(FACTORIAL_PROGRAM);
    }

    @Nested
    @DisplayName("词法分析测试")
    class LexerTests {

        @Test
        @DisplayName("测试词法分析器能够正确识别所有词法单元")
        void testLexerTokenization() {
            tokens = lexer.process(context);

            // 验证词法单元数量
            assertNotNull(tokens, "词法单元列表不应为空");
            assertTrue(tokens.size() > 0, "词法单元列表应包含词法单元");

            // 验证最后一个词法单元是 EOF
            assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).getType(), "最后一个词法单元应为 EOF");

            // 验证关键字识别
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.DEF), "应识别 'def' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.VAL), "应识别 'val' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.IF), "应识别 'if' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.THEN), "应识别 'then' 关键字");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.ELSE), "应识别 'else' 关键字");

            // 验证标识符识别
            List<String> identifiers = tokens.stream().filter(t -> t.getType() == TokenType.IDENTIFIER).map(Token::getValue).toList();

            assertTrue(identifiers.contains("factorial"), "应识别 'factorial' 标识符");
            assertTrue(identifiers.contains("n"), "应识别 'n' 标识符");
            assertTrue(identifiers.contains("x"), "应识别 'x' 标识符");
            assertTrue(identifiers.contains("print"), "应识别 'print' 标识符");

            // 验证操作符识别
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.MULTIPLY), "应识别 '*' 操作符");
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.LESS_EQUAL), "应识别 '<=' 操作符");

            // 验证变量引用前缀识别
            assertTrue(tokens.stream().anyMatch(t -> t.getType() == TokenType.AMPERSAND), "应识别 '&' 变量引用前缀");

            // 验证数字字面量识别
            List<Token> integers = tokens.stream().filter(t -> t.getType() == TokenType.INTEGER).toList();

            assertTrue(integers.stream().anyMatch(t -> t.getValue().equals("5")), "应识别 '5' 整数字面量");
            assertTrue(integers.stream().anyMatch(t -> t.getValue().equals("1")), "应识别 '1' 整数字面量");
        }
    }

    @Nested
    @DisplayName("语法分析测试")
    class ParserTests {

        private Parser parser;
        private Program ast;

        @BeforeEach
        void setUp() {
            tokens = lexer.process(context);
            parser = new Parser(tokens);
        }

        @Test
        @DisplayName("测试语法分析器能够正确构建 AST")
        void testParserAstConstruction() {
            ast = parser.process(context);

            // 验证 AST 不为空
            assertNotNull(ast, "AST 不应为空");

            // 验证 AST 包含正确数量的语句
            List<Stmt> statements = ast.getStatements();
            assertNotNull(statements, "语句列表不应为空");

            // 打印 AST 结构（用于调试）
            AstPrinter printer = new AstPrinter();
            String astString = ast.accept(printer);
            System.out.println("AST 结构：\n" + astString);

            // 验证函数声明
            assertTrue(statements.stream().anyMatch(s -> s instanceof FunctionDeclStmt && ((FunctionDeclStmt) s).getName().equals("factorial")), "AST 应包含 factorial 函数声明");

            // 验证 print 函数声明
            assertTrue(statements.stream().anyMatch(s -> s instanceof FunctionDeclStmt && ((FunctionDeclStmt) s).getName().equals("print")), "AST 应包含 print 函数声明");

            // 验证变量声明
            assertTrue(statements.stream().anyMatch(s -> s instanceof VarDeclStmt && ((VarDeclStmt) s).getName().equals("x")), "AST 应包含变量 x 的声明");

            // 验证函数调用
            assertTrue(statements.stream().anyMatch(s -> s instanceof ExpressionStmt && ((ExpressionStmt) s).getExpression() instanceof CallExpr && ((CallExpr) ((ExpressionStmt) s).getExpression()).getName().equals("print")), "AST 应包含 print 函数调用");
        }
    }

    @Test
    @DisplayName("测试多语句函数体解析")
    void testMultiStatementFunction() {
        // 创建新的测试上下文
        CompilationContext ctx = new CompilationContext(MULTI_STMT_FUNCTION);
        List<Token> tokens = new Lexer(MULTI_STMT_FUNCTION).process(ctx);
        Program ast = new Parser(tokens).process(ctx);

        // 打印 AST 结构（用于调试）
        AstPrinter printer = new AstPrinter();
        String astString = ast.accept(printer);
        System.out.println("AST 结构：\n" + astString);

        // 获取函数声明
        FunctionDeclStmt factorialFunc = ast.getStatements().stream().filter(s -> s instanceof FunctionDeclStmt).map(s -> (FunctionDeclStmt) s).filter(f -> f.getName().equals("factorial")).findFirst().orElseThrow();

        // 验证函数体是块表达式
        assertInstanceOf(BlockExpr.class, factorialFunc.getBody(), "函数体应为块表达式");

        // 获取块表达式中的语句
        BlockExpr block = (BlockExpr) factorialFunc.getBody();
        List<Stmt> stmts = block.getBlock().getStatements();

        // 验证包含三个语句（if表达式会被转换为ExpressionStmt）
        assertEquals(2, stmts.size(), "函数体应包含两个表达式语句");

        // 验证最后一个语句是变量引用表达式
        assertInstanceOf(ExpressionStmt.class, stmts.get(1), "第二个语句应为表达式语句");
    }

    @Test
    @DisplayName("测试嵌套函数调用解析")
    void testNestedFunctionCall() {
        // 创建新的测试上下文
        CompilationContext ctx = new CompilationContext(FACTORIAL_PROGRAM);
        List<Token> tokens = new Lexer(FACTORIAL_PROGRAM).process(ctx);
        Program ast = new Parser(tokens).process(ctx);

        // 打印 AST 结构（用于调试）
        AstPrinter printer = new AstPrinter();
        String astString = ast.accept(printer);
        System.out.println("嵌套函数调用 AST 结构：\n" + astString);

        // 获取最后一个语句（应该是 print 函数调用）
        Stmt lastStmt = ast.getStatements().get(ast.getStatements().size() - 1);
        assertInstanceOf(ExpressionStmt.class, lastStmt, "最后一个语句应为表达式语句");

        ExpressionStmt exprStmt = (ExpressionStmt) lastStmt;
        assertInstanceOf(CallExpr.class, exprStmt.getExpression(), "表达式应为函数调用");

        CallExpr printCall = (CallExpr) exprStmt.getExpression();
        assertEquals("print", printCall.getName(), "函数名应为 print");
        assertEquals(1, printCall.getArguments().size(), "print 函数应有一个参数");

        // 验证 print 函数的参数是 factorial 函数调用
        Expr factorialCall = printCall.getArguments().get(0);
        assertInstanceOf(CallExpr.class, factorialCall, "print 函数的参数应为 factorial 函数调用");

        CallExpr factorialCallExpr = (CallExpr) factorialCall;
        assertEquals("factorial", factorialCallExpr.getName(), "嵌套函数名应为 factorial");
        assertEquals(1, factorialCallExpr.getArguments().size(), "factorial 函数应有一个参数");

        // 验证 factorial 函数的参数是变量引用
        Expr factorialArg = factorialCallExpr.getArguments().get(0);
        assertInstanceOf(VariableExpr.class, factorialArg, "factorial 函数的参数应为变量引用");

        VariableExpr varExpr = (VariableExpr) factorialArg;
        assertEquals("x", varExpr.getName(), "变量名应为 x");
    }
}