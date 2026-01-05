package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.CommandRegistry;

import static org.tabooproject.fluxon.FluxonTestUtil.assertBothEqual;

/**
 * Command 功能集成测试
 */
public class CommandTest {

    @Test
    public void testSimpleCommand() {
        CommandRegistry registry = new CommandRegistry();
        registry.register("give-item",
                (parser, token) -> {
                    parser.consume(TokenType.STRING, "Expected item name");
                    return parser.previous().getLexeme();
                },
                (env, data) -> "Gave item: " + data
        );

        assertBothEqual("Gave item: diamond",
                FluxonTestUtil.runSilent("give-item \"diamond\"",
                        ctx -> ctx.setCommandRegistry(registry),
                        env -> {}));
    }

    @Test
    public void testCommandWithMultipleArguments() {
        CommandRegistry registry = new CommandRegistry();
        registry.register("give-item",
                (parser, token) -> {
                    parser.consume(TokenType.STRING, "Expected item name");
                    String item = parser.previous().getLexeme();
                    parser.consume(TokenType.INTEGER, "Expected amount");
                    int amount = (int) parser.previous().getValue();
                    return new Object[]{item, amount};
                },
                (env, data) -> "Gave " + data[1] + "x " + data[0]
        );

        assertBothEqual("Gave 64x diamond",
                FluxonTestUtil.runSilent("give-item \"diamond\" 64",
                        ctx -> ctx.setCommandRegistry(registry),
                        env -> {}));
    }

    @Test
    public void testCommandInExpression() {
        CommandRegistry registry = new CommandRegistry();
        registry.register("get-value",
                (parser, token) -> null,
                (env, data) -> 42
        );

        assertBothEqual(52,
                FluxonTestUtil.runSilent("get-value + 10",
                        ctx -> ctx.setCommandRegistry(registry),
                        env -> {}));
    }

    @Test
    public void testCommandWithEnvironmentAccess() {
        CommandRegistry registry = new CommandRegistry();
        registry.register("get-value",
                (parser, token) -> null,
                (env, data) -> env.getRootVariables().get("testValue")
        );

        assertBothEqual(100,
                FluxonTestUtil.runSilent("get-value + 58",
                        ctx -> ctx.setCommandRegistry(registry),
                        env -> env.defineRootVariable("testValue", 42)));
    }

    @Test
    public void testCommandWithExpressionArgument() {
        class GiveItemData {
            String itemName;
            int amount;
        }
        CommandRegistry registry = new CommandRegistry();
        registry.register("give-item",
                (parser, token) -> {
                    String itemName = parser.consume(TokenType.STRING, "Expected item name").getLexeme();
                    int amount = (int) parser.consume(TokenType.INTEGER, "Expected amount").getValue();
                    GiveItemData data = new GiveItemData();
                    data.itemName = itemName;
                    data.amount = amount;
                    return data;
                },
                (env, data) -> "Gave " + data.amount + "x " + data.itemName
        );

        assertBothEqual("Gave 64x diamond",
                FluxonTestUtil.runSilent("give-item \"diamond\" 64",
                        ctx -> ctx.setCommandRegistry(registry),
                        env -> {}));
    }
}
