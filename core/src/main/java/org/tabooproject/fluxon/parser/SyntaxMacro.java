package org.tabooproject.fluxon.parser;

/**
 * 语法宏接口
 * <p>
 * 语法宏是一种可插拔的语法扩展机制，允许在解析器中注册自定义的表达式语法。
 * 每个语法宏负责：
 * <ul>
 *   <li>检测当前 token 序列是否匹配其语法模式</li>
 *   <li>解析匹配的语法并生成对应的 AST 节点</li>
 * </ul>
 *
 * <h3>优先级</h3>
 * <p>
 * 当多个语法宏可能匹配同一位置时，优先级决定哪个宏先尝试匹配。
 * 数值越大优先级越高。建议的优先级范围：
 * </p>
 * <ul>
 *   <li><b>1000-2000</b>: 用户自定义扩展（如 command）</li>
 *   <li><b>500-999</b>: 特殊语法扩展（如解构赋值）</li>
 *   <li><b>100-200</b>: 内置关键字（if, for, while 等）</li>
 *   <li><b>0-99</b>: 基础语法（字面量、标识符、分组）</li>
 * </ul>
 *
 * <h3>实现示例</h3>
 * <pre>{@code
 * public class IfSyntaxMacro implements SyntaxMacro {
 *     @Override
 *     public boolean matches(Parser parser) {
 *         return parser.check(TokenType.IF);
 *     }
 *
 *     @Override
 *     public Trampoline<ParseResult> parse(Parser parser, Continuation<ParseResult> cont) {
 *         return IfParser.parse(parser, cont);
 *     }
 *
 *     @Override
 *     public int priority() {
 *         return 100;
 *     }
 * }
 * }</pre>
 *
 * @see SyntaxMacroRegistry
 */
public interface SyntaxMacro {

    /**
     * 检查当前位置是否匹配此语法宏
     * <p>
     * 实现时可以使用 parser 的前瞻方法（如 {@code check()}, {@code peek()}）
     * 来检测 token 序列，但不应消费任何 token。
     *
     * @param parser 解析器实例
     * @return true 表示匹配，将调用 {@link #parse} 方法
     */
    boolean matches(Parser parser);

    /**
     * 解析语法并生成 AST 节点
     * <p>
     * 此方法在 {@link #matches} 返回 true 后被调用。
     * 实现应消费相关 token 并构建对应的 ParseResult。
     *
     * @param parser       解析器实例
     * @param continuation 继续函数，用于 CPS 风格的解析
     * @return Trampoline 包装的解析结果
     */
    Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation);

    /**
     * 获取此语法宏的优先级
     * <p>
     * 数值越大优先级越高，优先被尝试匹配。
     * 默认优先级为 100，适用于大多数内置关键字语法。
     *
     * @return 优先级数值
     */
    default int priority() {
        return 100;
    }
}
