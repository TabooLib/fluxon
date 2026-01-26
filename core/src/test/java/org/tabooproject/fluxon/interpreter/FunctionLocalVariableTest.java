package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.parser.SymbolEnvironment;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.statement.Block;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 函数局部变量解析测试
 *
 * @author sky
 */
public class FunctionLocalVariableTest {

    @Test
    public void testFunctionLocalVariableDefinition() {
        String source = "def test() = {\n" +
                "  myList = []\n" +
                "  for i in 1..3 {\n" +
                "    &myList::add(&i)\n" +
                "  }\n" +
                "  &myList\n" +
                "}\n" +
                "test()";

        CompilationContext ctx = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        ParsedScript script = Fluxon.parse(ctx, env);
        List<ParseResult> results = script.getResults();

        // 找到函数定义
        FunctionDefinition funcDef = null;
        for (ParseResult result : results) {
            if (result instanceof FunctionDefinition) {
                funcDef = (FunctionDefinition) result;
                break;
            }
        }

        assertNotNull(funcDef, "Should find function definition");
        assertEquals("test", funcDef.getName());

        // 检查局部变量集合
        Set<String> localVars = funcDef.getLocalVariables();
        System.out.println("Function local variables: " + localVars);

        // myList 应该在局部变量集合中
        assertTrue(localVars.contains("myList"), "myList should be a local variable, but got: " + localVars);

        // i 应该在局部变量集合中
        assertTrue(localVars.contains("i"), "i should be a local variable, but got: " + localVars);

        // 检查 SymbolEnvironment 的 rootVariables
        SymbolEnvironment symEnv = ctx.getAttribute("symbolEnvironment");
        if (symEnv != null) {
            System.out.println("Root variables: " + symEnv.getRootVariables().keySet());
            assertFalse(symEnv.getRootVariables().containsKey("myList"), "myList should NOT be a root variable");
        }
    }

    /**
     * 测试函数内部和顶层同名变量的场景
     * 模拟 effect.fs 中的情况：函数内有 particles = []，顶层也有 particles = ...
     */
    @Test
    public void testFunctionLocalVarWithSameNameAtTopLevel() {
        String source = "def buildShape() = {\n" +
                "  particles = []\n" +
                "  for y in 0..2 {\n" +
                "    for x in 0..2 {\n" +
                "      &particles::add(&x + &y * 10)\n" +
                "    }\n" +
                "  }\n" +
                "  &particles\n" +
                "}\n" +
                "particles = buildShape()\n" +  // 顶层也有同名变量
                "&particles";

        CompilationContext ctx = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        ParsedScript script = Fluxon.parse(ctx, env);
        List<ParseResult> results = script.getResults();

        // 找到函数定义
        FunctionDefinition funcDef = null;
        for (ParseResult result : results) {
            if (result instanceof FunctionDefinition) {
                funcDef = (FunctionDefinition) result;
                break;
            }
        }

        assertNotNull(funcDef, "Should find function definition");
        assertEquals("buildShape", funcDef.getName());

        // 检查函数局部变量
        Set<String> localVars = funcDef.getLocalVariables();
        System.out.println("Function 'buildShape' local variables: " + localVars);

        // particles 必须在函数的局部变量集合中，而不是作为根变量
        assertTrue(localVars.contains("particles"),
                "particles should be a local variable of function, but got: " + localVars);
        assertTrue(localVars.contains("y"), "y should be a local variable");
        assertTrue(localVars.contains("x"), "x should be a local variable");
    }

    /**
     * 测试带类型注解参数的函数
     * 更接近 effect.fs 的场景
     */
    @Test
    public void testFunctionWithTypedParameters() {
        String source = "def buildShape(radius: int, height: int) = {\n" +
                "  particles = []\n" +
                "  for y in 0..&height {\n" +
                "    for x in 0..&radius {\n" +
                "      &particles::add(&x + &y * 10)\n" +
                "    }\n" +
                "  }\n" +
                "  &particles\n" +
                "}\n" +
                "result = buildShape(3, 4)\n" +
                "&result";

        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowJavaConstruction(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        ParsedScript script = Fluxon.parse(ctx, env);
        List<ParseResult> results = script.getResults();

        // 找到函数定义
        FunctionDefinition funcDef = null;
        for (ParseResult result : results) {
            if (result instanceof FunctionDefinition) {
                funcDef = (FunctionDefinition) result;
                break;
            }
        }

        assertNotNull(funcDef, "Should find function definition");
        assertEquals("buildShape", funcDef.getName());

        // 检查参数
        System.out.println("Parameters: " + funcDef.getParameters());
        assertEquals(2, funcDef.getParameters().size());

        // 检查函数局部变量
        Set<String> localVars = funcDef.getLocalVariables();
        System.out.println("Function 'buildShape' local variables: " + localVars);

        // particles 必须在函数的局部变量集合中
        assertTrue(localVars.contains("particles"),
                "particles should be a local variable of function, but got: " + localVars);

        // 参数应该也在局部变量中
        assertTrue(localVars.contains("radius"), "radius should be a local variable");
        assertTrue(localVars.contains("height"), "height should be a local variable");

        // 循环变量
        assertTrue(localVars.contains("y"), "y should be a local variable");
        assertTrue(localVars.contains("x"), "x should be a local variable");
    }

    /**
     * 测试带多行参数的函数（模拟 effect.fs 的精确格式）
     */
    @Test
    public void testFunctionWithMultilineParameters() {
        String source = "def createShape(\n" +
                "    radius: int,\n" +
                "    height: int\n" +
                ") = {\n" +
                "  particles = []\n" +
                "  for y in 0..&height {\n" +
                "    for angle in 0..10 {\n" +
                "      &particles::add(&y + &angle)\n" +
                "    }\n" +
                "  }\n" +
                "  &particles\n" +
                "}\n" +
                "createShape(2, 3)";

        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowJavaConstruction(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        ParsedScript script = Fluxon.parse(ctx, env);
        List<ParseResult> results = script.getResults();

        // 找到函数定义
        FunctionDefinition funcDef = null;
        for (ParseResult result : results) {
            if (result instanceof FunctionDefinition) {
                funcDef = (FunctionDefinition) result;
                break;
            }
        }

        assertNotNull(funcDef, "Should find function definition");
        assertEquals("createShape", funcDef.getName());

        // 检查函数局部变量
        Set<String> localVars = funcDef.getLocalVariables();
        System.out.println("Function 'createShape' local variables: " + localVars);
        System.out.println("Parameters: " + funcDef.getParameters());

        // particles 必须在函数的局部变量集合中
        assertTrue(localVars.contains("particles"),
                "particles should be a local variable of function, but got: " + localVars);

        // 循环变量
        assertTrue(localVars.contains("y"), "y should be a local variable");
        assertTrue(localVars.contains("angle"), "angle should be a local variable");
    }

    /**
     * 测试 AssignExpression 的 position 值
     * 确保函数内部的变量赋值有正确的 position (>=0)，而不是 -1（根变量）
     */
    @Test
    public void testAssignExpressionPositionInFunction() {
        String source = "def test() = {\n" +
                "  myVar = []\n" +
                "  &myVar\n" +
                "}";

        CompilationContext ctx = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        ParsedScript script = Fluxon.parse(ctx, env);
        List<ParseResult> results = script.getResults();

        // 找到函数定义
        FunctionDefinition funcDef = null;
        for (ParseResult result : results) {
            if (result instanceof FunctionDefinition) {
                funcDef = (FunctionDefinition) result;
                break;
            }
        }

        assertNotNull(funcDef, "Should find function definition");

        // 获取函数体（应该是 Block）
        ParseResult body = funcDef.getBody();
        assertTrue(body instanceof Block, "Function body should be Block");
        Block block = (Block) body;

        // 找到赋值语句
        AssignExpression assignExpr = null;
        for (ParseResult stmt : block.getStatements()) {
            if (stmt instanceof ExpressionStatement) {
                ParseResult expr = ((ExpressionStatement) stmt).getExpression();
                if (expr instanceof AssignExpression) {
                    assignExpr = (AssignExpression) expr;
                    break;
                }
            }
        }

        assertNotNull(assignExpr, "Should find AssignExpression in function body");
        System.out.println("AssignExpression: " + assignExpr);
        System.out.println("AssignExpression.position = " + assignExpr.getPosition());

        // 关键断言：position 必须 >= 0（局部变量），而不是 -1（根变量）
        assertTrue(assignExpr.getPosition() >= 0,
                "Variable 'myVar' should have position >= 0 (local variable), but got: " + assignExpr.getPosition());
    }

    /**
     * 精确模拟 effect.fs 的场景
     */
    @Test
    public void testEffectFsScenario() {
        String source = "def createComplete3DShape(\n" +
                "    center: java.lang.String,\n" +
                "    radius: int,\n" +
                "    height: int,\n" +
                "    segments: int\n" +
                ") = {\n" +
                "  particles = []\n" +
                "\n" +
                "  for y in 0..&height {\n" +
                "      for angle in 0..&segments {\n" +
                "          a = &angle * 2 * 3.14159 / &segments\n" +
                "          &particles::add(&a)\n" +
                "      }\n" +
                "  }\n" +
                "\n" +
                "  &particles\n" +
                "}\n" +
                "\n" +
                "particles = createComplete3DShape(\"test\", 2, 6, 20)";

        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowJavaConstruction(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        ParsedScript script = Fluxon.parse(ctx, env);
        List<ParseResult> results = script.getResults();

        // 找到函数定义
        FunctionDefinition funcDef = null;
        for (ParseResult result : results) {
            if (result instanceof FunctionDefinition) {
                funcDef = (FunctionDefinition) result;
                break;
            }
        }

        assertNotNull(funcDef, "Should find function definition");
        assertEquals("createComplete3DShape", funcDef.getName());

        // 检查参数
        System.out.println("Parameters: " + funcDef.getParameters());
        assertEquals(4, funcDef.getParameters().size());

        // 检查函数局部变量
        Set<String> localVars = funcDef.getLocalVariables();
        System.out.println("Function local variables: " + localVars);

        // particles 必须在函数的局部变量集合中
        assertTrue(localVars.contains("particles"),
                "particles should be a local variable of function, but got: " + localVars);

        // 检查 AssignExpression 的 position
        ParseResult body = funcDef.getBody();
        assertTrue(body instanceof Block, "Function body should be Block");
        Block block = (Block) body;

        AssignExpression particlesAssign = null;
        for (ParseResult stmt : block.getStatements()) {
            if (stmt instanceof ExpressionStatement) {
                ParseResult expr = ((ExpressionStatement) stmt).getExpression();
                if (expr instanceof AssignExpression) {
                    AssignExpression ae = (AssignExpression) expr;
                    if ("particles".equals(ae.getName())) {
                        particlesAssign = ae;
                        break;
                    }
                }
            }
        }

        assertNotNull(particlesAssign, "Should find particles AssignExpression");
        System.out.println("particles AssignExpression.position = " + particlesAssign.getPosition());

        // 关键断言：position 必须 >= 0（局部变量）
        assertTrue(particlesAssign.getPosition() >= 0,
                "Function-local 'particles' should have position >= 0, but got: " + particlesAssign.getPosition());
    }

    /**
     * 测试环境中预先有同名根变量时的情况
     * 这可能是用户遇到的实际场景
     */
    @Test
    public void testFunctionLocalVarWithPreexistingRootVar() {
        String source = "def test() = {\n" +
                "  myVar = []\n" +
                "  &myVar\n" +
                "}\n" +
                "test()";

        CompilationContext ctx = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        // 预先定义同名的根变量！
        env.setRootVariable("myVar", "preexisting");

        ParsedScript script = Fluxon.parse(ctx, env);
        List<ParseResult> results = script.getResults();

        FunctionDefinition funcDef = null;
        for (ParseResult result : results) {
            if (result instanceof FunctionDefinition) {
                funcDef = (FunctionDefinition) result;
                break;
            }
        }

        assertNotNull(funcDef, "Should find function definition");
        Set<String> localVars = funcDef.getLocalVariables();
        System.out.println("Function local variables with preexisting root var: " + localVars);

        // 关键：即使环境中有同名根变量，函数内定义的也应该是局部变量
        assertTrue(localVars.contains("myVar"),
                "myVar should be a LOCAL variable in function, even if root var exists. Got: " + localVars);

        // 检查 AssignExpression 的 position
        Block block = (Block) funcDef.getBody();
        AssignExpression assignExpr = null;
        for (ParseResult stmt : block.getStatements()) {
            if (stmt instanceof ExpressionStatement) {
                ParseResult expr = ((ExpressionStatement) stmt).getExpression();
                if (expr instanceof AssignExpression) {
                    assignExpr = (AssignExpression) expr;
                    break;
                }
            }
        }

        assertNotNull(assignExpr, "Should find AssignExpression");
        System.out.println("AssignExpression.position with preexisting root var = " + assignExpr.getPosition());

        // 关键断言！
        assertTrue(assignExpr.getPosition() >= 0,
                "Even with preexisting root var, function-local 'myVar' should have position >= 0, but got: " + assignExpr.getPosition());
    }
}
