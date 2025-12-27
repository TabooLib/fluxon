package org.tabooproject.fluxon.parser.expression;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.CommandExecutor;

/**
 * Command 表达式
 * <p>
 * 表示在脚本中调用的自定义 command。Command 由开发者在解析阶段通过 {@link org.tabooproject.fluxon.parser.CommandRegistry} 注册，
 * 提供自定义的语法解析和执行逻辑。
 * </p>
 * <p>
 * 示例脚本：
 * <pre>
 * give-item "diamond" 64
 * set-property name "value"
 * </pre>
 * </p>
 * <p>
 * <b>注意：</b> Command 表达式仅支持解释执行，不支持编译为字节码。
 * </p>
 * 
 * @see org.tabooproject.fluxon.parser.CommandParser
 * @see org.tabooproject.fluxon.parser.CommandExecutor
 * @see org.tabooproject.fluxon.parser.CommandRegistry
 */
public class CommandExpression extends Expression {

    private final String commandName;
    private final Object parsedData;
    private final CommandExecutor<?> executor;

    /**
     * 创建 Command 表达式
     * 
     * @param commandName command 名称
     * @param parsedData  解析器返回的数据（可以是任意类型）
     * @param executor    执行器（在解析时捕获，保证一致性）
     */
    public CommandExpression(@NotNull String commandName, Object parsedData, @NotNull CommandExecutor<?> executor) {
        super(ExpressionType.COMMAND);
        if (commandName.isEmpty()) {
            throw new IllegalArgumentException("Command name cannot be null or empty");
        }
        this.commandName = commandName;
        this.parsedData = parsedData;
        this.executor = executor;
    }

    /**
     * 获取 command 名称
     * 
     * @return command 名称
     */
    @NotNull
    public String getCommandName() {
        return commandName;
    }

    /**
     * 获取解析器返回的数据
     * <p>
     * 解析数据的类型和结构由 {@link org.tabooproject.fluxon.parser.CommandParser} 实现决定。
     * 执行器应该知道如何解释这个数据。
     * </p>
     * 
     * @return 解析数据（可能为 null）
     */
    public Object getParsedData() {
        return parsedData;
    }

    /**
     * 获取执行器
     * <p>
     * 执行器在解析时被捕获，保证解析和执行使用相同的 command 定义。
     * </p>
     * 
     * @return CommandExecutor 实例
     */
    @NotNull
    public CommandExecutor<?> getExecutor() {
        return executor;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.COMMAND;
    }

    @Override
    public String toString() {
        return "Command(" + commandName + ", " + parsedData + ")";
    }

    @Override
    public String toPseudoCode() {
        return commandName + " " + (parsedData != null ? parsedData.toString() : "");
    }
}
