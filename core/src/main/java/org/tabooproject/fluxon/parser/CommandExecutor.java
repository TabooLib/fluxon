package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.runtime.Environment;

/**
 * Command 执行器接口
 * <p>
 * 定义 command 在运行时的执行逻辑。与 {@link CommandParser} 分离职责：
 * CommandParser 负责解析，CommandExecutor 负责执行。
 * </p>
 *
 * <h3>泛型参数</h3>
 * <p>
 * 类型参数 {@code <T>} 必须与对应 {@link CommandParser} 的类型参数一致。
 * 这确保了解析阶段产生的数据类型与执行阶段期望的数据类型匹配。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 简单 command：直接使用解析数据
 * CommandExecutor<String> giveItemExecutor = (env, itemName) -> {
 *     Object player = env.getRootVariables().get("player");
 *     giveItem(player, itemName, 1);
 *     return "Gave 1x " + itemName;
 * };
 *
 * // 带表达式的 command：使用 Supplier 延迟求值
 * class GiveItemData {
 *     String itemName;
 *     Supplier<Object> amountExpr;  // 解析时包装为 Supplier
 * }
 *
 * CommandExecutor<GiveItemData> giveItemExecutor = (env, data) -> {
 *     int amount = (int) data.amountExpr.get();  // 执行时求值
 *     return "Gave " + amount + "x " + data.itemName;
 * };
 *
 * // 注册到 registry
 * CommandRegistry.primary().register("give-item", parser, giveItemExecutor);
 * }</pre>
 *
 * <h3>线程安全</h3>
 * <p>
 * CommandExecutor 实例可能在多线程环境下被并发调用，应保证无状态或正确处理并发访问。
 * 建议使用函数式风格实现，避免可变状态。
 * </p>
 *
 * @param <T> 解析数据的类型，必须与对应 CommandParser 的类型参数一致
 */
@FunctionalInterface
public interface CommandExecutor<T> {

    /**
     * 执行 command 的业务逻辑
     * <p>
     * 此方法在运行时执行到 CommandExpression 时被调用。
     * 可以访问运行时环境（变量、函数等），执行自定义的业务逻辑。
     * </p>
     *
     * @param environment 运行时环境，可用于访问变量和函数
     * @param parsedData  CommandParser 返回的解析结果，类型为 T
     * @return 执行结果，可以是任意 Java 对象
     * @throws Exception 当执行过程中遇到错误时
     */
    Object execute(Environment environment, T parsedData) throws Exception;
}
