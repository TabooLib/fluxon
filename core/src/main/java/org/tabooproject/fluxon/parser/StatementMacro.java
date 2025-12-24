package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.parser.definition.Annotation;

import java.util.List;

/**
 * 语句宏接口
 * <p>
 * 用于扩展语句解析，支持顶层语句和子语句两种模式。
 *
 * <h3>顶层语句 vs 子语句</h3>
 * <ul>
 *   <li><b>顶层语句</b>：脚本最外层，可以有注解，如函数定义</li>
 *   <li><b>子语句</b>：块内部，如 return、break、continue</li>
 * </ul>
 *
 * <h3>优先级</h3>
 * <p>
 * 数值越大越优先匹配。建议范围：
 * </p>
 * <ul>
 *   <li>用户扩展: 500-2000</li>
 *   <li>函数定义: 200</li>
 *   <li>控制流语句: 100</li>
 *   <li>表达式语句: 0 (兜底)</li>
 * </ul>
 */
public interface StatementMacro {

    /**
     * 检查是否匹配顶层语句
     *
     * @param parser 解析器
     * @return true 表示匹配
     */
    default boolean matchesTopLevel(Parser parser) {
        return false;
    }

    /**
     * 检查是否匹配子语句
     *
     * @param parser 解析器
     * @return true 表示匹配
     */
    default boolean matchesSub(Parser parser) {
        return false;
    }

    /**
     * 解析顶层语句
     *
     * @param parser      解析器
     * @param annotations 已解析的注解列表
     * @return 解析结果
     */
    default ParseResult parseTopLevel(Parser parser, List<Annotation> annotations) {
        throw new UnsupportedOperationException("This macro does not support top-level statements");
    }

    /**
     * 解析子语句（CPS 版本）
     *
     * @param parser       解析器
     * @param continuation 继续函数
     * @return 解析结果
     */
    default Trampoline<ParseResult> parseSub(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        throw new UnsupportedOperationException("This macro does not support sub-statements");
    }

    /**
     * 获取优先级
     *
     * @return 优先级，数值越大越优先
     */
    default int priority() {
        return 100;
    }
}
