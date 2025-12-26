package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.CommandRegistry;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.CommandExpression;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Command 功能集成测试
 */
public class CommandIntegrationTest {

    @Test
    public void testSimpleCommand() {
        // 注册一个简单的 command
        CommandRegistry registry = new CommandRegistry();
        registry.register("give-item",
                (parser, token) -> {
                    // 解析参数: give-item "item_name"
                    parser.consume(TokenType.STRING, "Expected item name");
                    return parser.previous().getLexeme();
                },
                (interpreter, data) -> "Gave item: " + data
        );

        CompilationContext context = new CompilationContext("give-item \"diamond\"");
        context.setCommandRegistry(registry);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        List<ParseResult> parseResults = Fluxon.parse(env, context);
        Object result = Fluxon.eval(parseResults, env);

        assertEquals("Gave item: diamond", result);
    }

    @Test
    public void testCommandWithMultipleArguments() {
        // 注册一个带多参数的 command
        CommandRegistry registry = new CommandRegistry();
        registry.register("give-item",
                (parser, token) -> {
                    // 解析: give-item "item" amount
                    parser.consume(TokenType.STRING, "Expected item name");
                    String item = parser.previous().getLexeme();
                    parser.consume(TokenType.INTEGER, "Expected amount");
                    int amount = (int) parser.previous().getValue();
                    return new Object[]{item, amount};
                },
                (interpreter, data) -> "Gave " + data[1] + "x " + data[0]
        );

        CompilationContext context = new CompilationContext("give-item \"diamond\" 64");
        context.setCommandRegistry(registry);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        List<ParseResult> parseResults = Fluxon.parse(env, context);
        Object result = Fluxon.eval(parseResults, env);

        assertEquals("Gave 64x diamond", result);
    }

    @Test
    public void testCommandInExpression() {
        // 测试 command 在表达式中的使用
        CommandRegistry registry = new CommandRegistry();
        registry.register("get-value",
                (parser, token) -> null,
                (interpreter, data) -> 42
        );

        CompilationContext context = new CompilationContext("get-value + 10");
        context.setCommandRegistry(registry);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        List<ParseResult> parseResults = Fluxon.parse(env, context);
        Object result = Fluxon.eval(parseResults, env);

        assertEquals(52, result);
    }

    @Test
    public void testCommandWithEnvironmentAccess() {
        CommandRegistry registry = new CommandRegistry();
        registry.register("get-value",
                (parser, token) -> null,
                (interpreter, data) -> {
                    // 从环境中获取预定义的值
                    return interpreter.getEnvironment().getRootVariables().get("testValue");
                }
        );

        CompilationContext context = new CompilationContext("get-value + 58");
        context.setCommandRegistry(registry);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("testValue", 42);

        List<ParseResult> parseResults = Fluxon.parse(env, context);
        Object result = Fluxon.eval(parseResults, env);
        assertEquals(100, result);
    }

    @Test
    public void testIndependentRegistries() {
        // 创建两个独立的 registry
        CommandRegistry registry1 = new CommandRegistry();
        CommandRegistry registry2 = new CommandRegistry();

        // 在 registry1 注册 foo
        registry1.register("foo",
                (parser, token) -> "foo-data",
                (interpreter, data) -> "foo-result"
        );

        // 在 registry2 注册 bar
        registry2.register("bar",
                (parser, token) -> "bar-data",
                (interpreter, data) -> "bar-result"
        );

        // 使用 registry1 解析 foo（应该成功）
        CompilationContext ctx1 = new CompilationContext("foo");
        ctx1.setCommandRegistry(registry1);
        Environment env1 = FluxonRuntime.getInstance().newEnvironment();
        List<ParseResult> results1 = Fluxon.parse(env1, ctx1);
        assertEquals(1, results1.size());
        // 顶层表达式被包装在 ExpressionStatement 中
        assertInstanceOf(ExpressionStatement.class, results1.get(0));
        ParseResult innerExpr1 = ((ExpressionStatement) results1.get(0)).getExpression();
        assertInstanceOf(CommandExpression.class, innerExpr1);
        assertEquals("foo", ((CommandExpression) innerExpr1).getCommandName());

        // 验证执行结果
        Object result1 = Fluxon.eval(results1, env1);
        assertEquals("foo-result", result1);

        // 使用 registry2 解析 bar（应该成功）
        CompilationContext ctx2 = new CompilationContext("bar");
        ctx2.setCommandRegistry(registry2);
        Environment env2 = FluxonRuntime.getInstance().newEnvironment();
        List<ParseResult> results2 = Fluxon.parse(env2, ctx2);
        assertEquals(1, results2.size());
        assertInstanceOf(ExpressionStatement.class, results2.get(0));
        ParseResult innerExpr2 = ((ExpressionStatement) results2.get(0)).getExpression();
        assertInstanceOf(CommandExpression.class, innerExpr2);
        assertEquals("bar", ((CommandExpression) innerExpr2).getCommandName());

        // 验证执行结果
        Object result2 = Fluxon.eval(results2, env2);
        assertEquals("bar-result", result2);

        // 使用 registry1 解析 bar（应该失败，解析为标识符）
        CompilationContext ctx3 = new CompilationContext("bar");
        ctx3.setCommandRegistry(registry1);
        List<ParseResult> results3 = Fluxon.parse(FluxonRuntime.getInstance().newEnvironment(), ctx3);
        assertEquals(1, results3.size());
        assertInstanceOf(ExpressionStatement.class, results3.get(0));
        ParseResult innerExpr3 = ((ExpressionStatement) results3.get(0)).getExpression();
        // bar 不是 command，应该解析为标识符
        assertFalse(innerExpr3 instanceof CommandExpression);
    }

    @Test
    public void testCommandWithExpressionArgument() {
        // 定义数据类来保存解析结果
        class GiveItemData {
            String itemName;
            ParseResult amountExpr;
        }
        CommandRegistry registry = new CommandRegistry();
        registry.register("give-item",
                (parser, token) -> {
                    // 解析物品名称（字符串字面量）
                    String itemName = parser.consume(TokenType.STRING, "Expected item name").getLexeme();
                    // 解析数量（Fluxon 表达式）
                    ParseResult amountExpr = parser.parseExpression();
                    GiveItemData data = new GiveItemData();
                    data.itemName = itemName;
                    data.amountExpr = amountExpr;
                    return data;
                },
                (interpreter, data) -> {
                    // 计算表达式的值
                    int amount = (int) interpreter.evaluate(data.amountExpr);
                    return "Gave " + amount + "x " + data.itemName;
                }
        );

        CompilationContext context = new CompilationContext("give-item \"diamond\" (32 + 32)");
        context.setCommandRegistry(registry);

        Environment env = FluxonRuntime.getInstance().newEnvironment();
        List<ParseResult> parseResults = Fluxon.parse(env, context);
        Object result = Fluxon.eval(parseResults, env);

        assertEquals("Gave 64x diamond", result);
    }
}
