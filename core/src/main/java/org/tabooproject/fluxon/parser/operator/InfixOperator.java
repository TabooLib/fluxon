package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.Trampoline;

/**
 * 中缀运算符接口
 * <p>
 * 用于 Pratt Parser 风格的运算符优先级解析。
 * 每个中缀运算符定义其绑定力（binding power）来确定优先级。
 *
 * <h3>绑定力规则</h3>
 * <ul>
 *   <li>绑定力越高，优先级越高</li>
 *   <li>左结合运算符：右侧使用 bindingPower + 1</li>
 *   <li>右结合运算符：右侧使用 bindingPower</li>
 * </ul>
 *
 * <h3>默认绑定力参考</h3>
 * <pre>
 * 10  - Assignment (=, +=, -=, ...)  右结合
 * 20  - Elvis (?:)                   右结合
 * 30  - Ternary (? :)                右结合
 * 40  - LogicalOr (||)               左结合
 * 50  - LogicalAnd (&&)              左结合
 * 60  - Range (.., ..<)              左结合
 * 70  - Equality (==, !=)            左结合
 * 80  - Comparison (>, >=, <, <=)    左结合
 * 90  - Term (+, -)                  左结合
 * 100 - Factor (*, /, %)             左结合
 * </pre>
 */
public interface InfixOperator {

    /**
     * 获取左绑定力
     * <p>
     * 决定此运算符与左侧表达式的结合强度。
     * 当前运算符的绑定力必须大于等于最小绑定力才会被匹配。
     *
     * @return 左绑定力值
     */
    int bindingPower();

    /**
     * 检查当前位置是否匹配此运算符
     *
     * @param parser 解析器实例
     * @return true 表示匹配
     */
    boolean matches(Parser parser);

    /**
     * 解析运算符右侧并构建表达式
     *
     * @param parser       解析器实例
     * @param left         左侧表达式
     * @param operator     运算符 token
     * @param continuation 继续函数
     * @return 解析结果
     */
    Trampoline<ParseResult> parse(
            Parser parser,
            ParseResult left,
            Token operator,
            Trampoline.Continuation<ParseResult> continuation
    );
}
