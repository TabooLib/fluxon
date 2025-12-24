package org.tabooproject.fluxon.parser;

/**
 * 后缀运算符接口
 * <p>
 * 处理表达式后的操作符，如索引访问 {@code []}、后缀函数调用 {@code ()} 等。
 * 后缀运算符在 primary 表达式解析完成后循环应用。
 *
 * <h3>扩展示例</h3>
 * <pre>{@code
 * // 添加安全导航操作符 ?.
 * public class SafeNavigationPostfixOperator implements PostfixOperator {
 *     @Override
 *     public boolean matches(Parser parser, ParseResult expr) {
 *         return parser.check(TokenType.QUESTION_DOT);
 *     }
 *
 *     @Override
 *     public ParseResult parse(Parser parser, ParseResult expr) {
 *         parser.consume(); // consume ?.
 *         String name = parser.consume(TokenType.IDENTIFIER).getLexeme();
 *         return new SafeNavigationExpression(expr, name);
 *     }
 * }
 * }</pre>
 */
public interface PostfixOperator {

    /**
     * 检查是否匹配此后缀运算符
     *
     * @param parser 解析器
     * @param expr   当前表达式（某些后缀操作可能需要检查表达式类型）
     * @return 是否匹配
     */
    boolean matches(Parser parser, ParseResult expr);

    /**
     * 解析后缀操作并返回新的表达式
     *
     * @param parser 解析器
     * @param expr   被操作的表达式
     * @return 应用后缀操作后的表达式
     */
    ParseResult parse(Parser parser, ParseResult expr);

    /**
     * 优先级（数值越大越先尝试匹配）
     * <p>
     * 建议范围：
     * <ul>
     *   <li>用户扩展: 50-200</li>
     *   <li>内置操作: 0-50</li>
     * </ul>
     */
    default int priority() {
        return 0;
    }
}
