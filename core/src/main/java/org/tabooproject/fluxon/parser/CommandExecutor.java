package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntimeError;

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
 * // 定义解析结果的数据类
 * class GiveItemData {
 *     String itemName;
 *     ParseResult amountExpr;
 * }
 * 
 * // 创建类型安全的执行器
 * CommandExecutor<GiveItemData> giveItemExecutor = (interpreter, data) -> {
 *     // 从环境中获取玩家对象
 *     Object player = interpreter.getEnvironment().getRootVariables().get("player");
 *     // 计算数量表达式
 *     int amount = (int) interpreter.eval(data.amountExpr);
 *     // 执行给予物品的逻辑
 *     giveItem(player, data.itemName, amount);
 *     // 返回结果
 *     return "Gave " + amount + "x " + data.itemName;
 * };
 * 
 * // 注册到 registry
 * CommandRegistry.primary().register("give-item", parser, giveItemExecutor);
 * }</pre>
 * 
 * <h3>异常处理</h3>
 * <p>
 * 执行过程中遇到错误时，应抛出 {@link FluxonRuntimeError} 或其子类。
 * Interpreter 会自动附加源码位置信息（SourceTrace），便于错误定位。
 * </p>
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
     * 此方法在 Interpreter 遍历 AST 执行到 CommandExpression 时被调用。
     * 可以访问运行时环境（变量、函数等），执行自定义的业务逻辑。
     * </p>
     * 
     * <p>
     * <b>重要提示:</b>
     * <ul>
     *   <li>parsedData 是 {@link CommandParser#parse} 方法返回的对象，类型为 T</li>
     *   <li>可以通过 environment 读写变量、调用函数等</li>
     *   <li>执行失败时应抛出 {@link FluxonRuntimeError}，包含详细的错误信息</li>
     *   <li>返回值将作为该表达式的执行结果，可以用于赋值、运算等</li>
     * </ul>
     * </p>
     * 
     * @param interpreter 当前执行的解释器实例
     * @param parsedData CommandParser 返回的解析结果，类型为 T
     * @return 执行结果，可以是任意 Java 对象
     * @throws FluxonRuntimeError 当执行过程中遇到运行时错误时
     */
    Object execute(Interpreter interpreter, T parsedData) throws FluxonRuntimeError;
}
