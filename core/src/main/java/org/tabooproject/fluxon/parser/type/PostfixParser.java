package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.PostfixOperator;
import org.tabooproject.fluxon.parser.PostfixOperatorRegistry;

/**
 * 后缀操作解析器
 * <p>
 * 委托给 {@link PostfixOperatorRegistry} 处理后缀操作。
 */
public class PostfixParser {

    /**
     * 处理后缀操作（索引访问、函数调用等）
     *
     * @param parser 解析器
     * @param expr   已解析的表达式
     * @return 应用后缀操作后的表达式
     */
    public static ParseResult parsePostfixOperations(Parser parser, ParseResult expr) {
        PostfixOperatorRegistry registry = parser.getContext().getPostfixOperatorRegistry();
        while (true) {
            PostfixOperator op = registry.findMatch(parser, expr);
            if (op == null) {
                break;
            }
            expr = op.parse(parser, expr);
        }
        return expr;
    }
}
