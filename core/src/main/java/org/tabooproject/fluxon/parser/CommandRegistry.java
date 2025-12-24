package org.tabooproject.fluxon.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Command 注册表
 * <p>
 * 管理自定义 command 的注册和查找。支持两种使用模式：
 * </p>
 * <ul>
 *   <li><b>静态主注册表</b> - 通过 {@link #primary()} 访问的全局注册表，适用于大多数场景</li>
 *   <li><b>独立实例</b> - 通过构造函数创建的独立注册表，用于沙箱隔离或多租户场景</li>
 * </ul>
 *
 * <h3>使用示例 - 静态主注册表</h3>
 * <pre>{@code
 * // 注册 command 到主注册表
 * CommandRegistry.primary().register("give-item",
 *     (parser, token) -> {
 *         String item = parser.consume(TokenType.STRING).getLiteral();
 *         return Map.of("item", item);
 *     },
 *     (interpreter, data) -> {
 *         Map<String, Object> params = (Map<String, Object>) data;
 *         return "Gave " + params.get("item");
 *     }
 * );
 *
 * // 使用默认配置解析脚本（自动使用主注册表）
 * Fluxon fluxon = new Fluxon();
 * fluxon.eval("give-item \"diamond\"");
 * }</pre>
 *
 * <h3>使用示例 - 独立注册表</h3>
 * <pre>{@code
 * // 创建独立注册表
 * CommandRegistry sandboxRegistry = new CommandRegistry();
 * sandboxRegistry.register("safe-command", parser, executor);
 *
 * // 使用自定义注册表
 * CompilationContext context = new CompilationContext(source);
 * context.setCommandRegistry(sandboxRegistry);
 *
 * Fluxon fluxon = new Fluxon();
 * fluxon.eval(source, context);
 * }</pre>
 *
 * <h3>线程安全</h3>
 * <p>
 * CommandRegistry 实例在注册阶段（通常在应用启动时）应在单线程环境下操作。
 * 注册完成后，多个线程可以并发调用 {@link #hasCommand} 和 {@link #get} 方法进行只读访问，无需额外同步。
 * </p>
 *
 * <p>
 * 如果需要在运行时动态注册 command（不推荐），应由应用层保证线程安全（如使用 ConcurrentHashMap 或外部同步）。
 * </p>
 *
 */
public class CommandRegistry {

    /**
     * 静态主注册表实例
     */
    private static final CommandRegistry PRIMARY = new CommandRegistry();

    /**
     * Command 映射表：commandName -> CommandHandler
     */
    private final Map<String, CommandHandler<?>> commands;

    /**
     * 获取静态主注册表
     * <p>
     * 这是全局默认的 command 注册表，适用于大多数场景。
     * 如果 {@link org.tabooproject.fluxon.compiler.CompilationContext} 未显式设置 CommandRegistry，
     * 将自动使用此主注册表。
     * </p>
     *
     * @return 主注册表实例
     */
    @NotNull
    public static CommandRegistry primary() {
        return PRIMARY;
    }

    /**
     * 创建一个包含主注册表所有 command 的新注册表实例
     * <p>
     * 用于在主注册表的基础上创建隔离的注册表副本。
     * </p>
     *
     * @return 主注册表命令的副本
     */
    public static CommandRegistry withDefaults() {
        CommandRegistry registry = new CommandRegistry();
        registry.commands.putAll(primary().commands);
        return registry;
    }

    /**
     * 创建新的独立 CommandRegistry 实例
     */
    public CommandRegistry() {
        this.commands = new HashMap<>();
    }

    /**
     * 注册 command（泛型版本）
     * <p>
     * 将指定名称的 command 及其解析器、执行器注册到此注册表。
     * 如果 command 名称已存在，将覆盖之前的注册。
     * </p>
     *
     * <h3>泛型参数</h3>
     * <p>
     * 类型参数 {@code <T>} 确保 CommandParser 和 CommandExecutor 使用相同的数据类型，
     * 提供编译期类型安全，避免运行时类型转换错误。
     * </p>
     *
     * @param <T>         解析数据的类型
     * @param commandName command 名称（标识符），如 "give-item"
     * @param parser      解析器，负责解析 command 的参数和语法
     * @param executor    执行器，负责执行 command 的业务逻辑
     * @throws IllegalArgumentException 如果任何参数为 null
     */
    public <T> void register(@NotNull String commandName, @NotNull CommandParser<T> parser, @NotNull CommandExecutor<T> executor) {
        if (commandName.isEmpty()) {
            throw new IllegalArgumentException("Command name cannot be null or empty");
        }
        commands.put(commandName, new CommandHandler<>(parser, executor));
    }

    /**
     * 检查 command 是否已注册
     *
     * @param commandName command 名称
     * @return 如果 command 已注册返回 true，否则返回 false
     */
    public boolean hasCommand(@NotNull String commandName) {
        return commands.containsKey(commandName);
    }

    /**
     * 获取已注册的 command 处理器
     *
     * @param commandName command 名称
     * @return 对应的 CommandHandler，如果未注册返回 null
     */
    @Nullable
    public CommandHandler<?> get(@NotNull String commandName) {
        return commands.get(commandName);
    }

    /**
     * 获取所有已注册的 command 名称
     * <p>
     * 返回的 Map 是只读的，修改不会影响注册表。
     * </p>
     *
     * @return 不可变的 command 映射表
     */
    @NotNull
    public Map<String, CommandHandler<?>> getCommands() {
        return new HashMap<>(commands);
    }

    /**
     * 清空所有已注册的 command
     * <p>
     * 警告：此操作不可逆，主要用于测试环境。生产环境应避免使用。
     * </p>
     */
    public void clear() {
        commands.clear();
    }
}