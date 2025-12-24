package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.error.ParseException;

/**
 * Command 解析器接口
 * <p>
 * 允许开发者注册自定义语法处理器，在脚本解析阶段完全控制 token 解析过程。
 * 这是一种高自由度的语法扩展机制，用于支持类似 Kether 的 DSL 语法。
 * </p>
 * 
 * <h3>泛型参数</h3>
 * <p>
 * 类型参数 {@code <T>} 表示解析结果的类型，这个类型会在执行阶段传递给 {@link CommandExecutor}。
 * 使用泛型可以避免在 CommandExecutor 中进行类型转换，提升开发体验和类型安全。
 * </p>
 * 
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 定义解析结果的数据类
 * class GiveItemData {
 *     String itemName;
 *     int amount;
 * }
 * 
 * // 创建类型安全的 CommandParser
 * CommandParser<GiveItemData> giveItemParser = (parser, commandToken) -> {
 *     // 解析物品名称（字符串字面量）
 *     String itemName = parser.consume(TokenType.STRING, "Expected item name").getLexeme();
 *     // 解析数量（表达式）
 *     ParseResult amountExpr = parser.parseExpression();
 *     
 *     GiveItemData data = new GiveItemData();
 *     data.itemName = itemName;
 *     data.amount = 1; // 默认值，实际值在执行时从 amountExpr 计算
 *     return data;
 * };
 * 
 * // 创建类型安全的 CommandExecutor
 * CommandExecutor<GiveItemData> executor = (interpreter, data) -> {
 *     // 不需要类型转换！
 *     return "Gave " + data.amount + "x " + data.itemName;
 * };
 * 
 * // 注册 command
 * CommandRegistry.primary().register("give-item", giveItemParser, executor);
 * 
 * // 脚本中使用
 * // give-item "diamond" (player.level * 2)
 * }</pre>
 * 
 * <h3>解析表达式</h3>
 * <p>
 * 可以使用 {@link Parser#parseExpression()} 方法在 command 中解析 Fluxon 表达式。
 * 这允许 command 的参数支持复杂的计算和变量引用。
 * </p>
 * 
 * <h3>线程安全</h3>
 * <p>
 * CommandParser 实例在多线程环境下被并发调用时，应保证无状态或正确处理并发访问。
 * 建议使用函数式风格实现，避免可变状态。
 * </p>
 * 
 * @param <T> 解析结果的类型，会传递给对应的 CommandExecutor
 */
@FunctionalInterface
public interface CommandParser<T> {
    
    /**
     * 解析 command 的参数和内容
     * <p>
     * 此方法在 Parser 识别到 command 名称后被调用，允许开发者完全控制后续 token 的解析过程。
     * 可以调用 Parser 的各种方法（如 consume, match, peek, advance 等）来解析自定义语法。
     * </p>
     * 
     * <p>
     * <b>重要提示:</b>
     * <ul>
     *   <li>解析失败时应抛出 {@link ParseException}，包含详细的错误信息和位置</li>
     *   <li>返回的对象将在执行阶段传递给 {@link CommandExecutor}</li>
     *   <li>可以调用 {@link Parser#parseExpression()} 解析 Fluxon 表达式</li>
     *   <li>必须正确处理 token 流，避免解析不完整导致后续语法错误</li>
     * </ul>
     * </p>
     * 
     * @param parser Parser 实例，提供 token 流访问和解析工具方法
     * @param commandToken 触发此 command 的 token，包含 command 名称和源码位置信息
     * @return 解析结果，类型为 T
     * @throws ParseException 当解析过程中遇到语法错误时
     */
    T parse(Parser parser, Token commandToken) throws ParseException;
}
