package org.tabooproject.fluxon.parser;

/**
 * Command 处理器包装类
 * <p>
 * 封装 {@link CommandParser} 和 {@link CommandExecutor}，
 * 简化 {@link CommandRegistry} 的内部管理。
 * </p>
 * 
 * <h3>泛型参数</h3>
 * <p>
 * 类型参数 {@code <T>} 确保 CommandParser 和 CommandExecutor 的类型参数一致。
 * 这在编译期保证了类型安全，避免运行时类型转换错误。
 * </p>
 * 
 * @param <T> 解析数据的类型
 */
public class CommandHandler<T> {
    
    private final CommandParser<T> parser;
    private final CommandExecutor<T> executor;
    
    /**
     * 创建 CommandHandler
     * 
     * @param parser 解析器，负责解析 command 语法
     * @param executor 执行器，负责执行 command 业务逻辑
     */
    public CommandHandler(CommandParser<T> parser, CommandExecutor<T> executor) {
        if (parser == null) {
            throw new IllegalArgumentException("CommandParser cannot be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("CommandExecutor cannot be null");
        }
        this.parser = parser;
        this.executor = executor;
    }
    
    /**
     * 获取解析器
     * 
     * @return CommandParser 实例
     */
    public CommandParser<T> getParser() {
        return parser;
    }
    
    /**
     * 获取执行器
     * 
     * @return CommandExecutor 实例
     */
    public CommandExecutor<T> getExecutor() {
        return executor;
    }
}
