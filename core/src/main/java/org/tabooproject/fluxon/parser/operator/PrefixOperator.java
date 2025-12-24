package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.Trampoline;

/**
 * 前缀运算符接口
 * <p>
 * 用于处理表达式开头的一元运算符，如 !, -, await, & 等。
 * 前缀运算符在解析 primary 表达式之前被检查。
 *
 * <h3>优先级</h3>
 * <p>
 * 前缀运算符通过 {@link #priority()} 方法定义优先级，
 * 优先级高的运算符先尝试匹配。
 * </p>
 *
 * <h3>默认优先级参考</h3>
 * <pre>
 * 100 - Reference (&, &?)
 * 50  - Unary (!, -)
 * 50  - Await (await)
 * </pre>
 */
public interface PrefixOperator {

    /**
     * 检查当前位置是否匹配此前缀运算符
     *
     * @param parser 解析器实例
     * @return true 表示匹配
     */
    boolean matches(Parser parser);

    /**
     * 解析前缀运算符及其操作数
     *
     * @param parser       解析器实例
     * @param continuation 继续函数
     * @return 解析结果
     */
    Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation);

    /**
     * 获取优先级
     * <p>
     * 优先级高的前缀运算符先尝试匹配。
     *
     * @return 优先级值
     */
    default int priority() {
        return 50;
    }
}
