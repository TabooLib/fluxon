package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.ContextCallExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.type.BlockParser;
import org.tabooproject.fluxon.parser.type.FunctionCallParser;
import org.tabooproject.fluxon.parser.type.PostfixParser;

/**
 * 上下文调用运算符 (:: 和 ?::)
 * <p>
 * 绑定力: 110，左结合（高于函数调用，类似于属性访问）
 * <p>
 * 语法示例：
 * <ul>
 *   <li>{@code time :: formatTimestamp(...)} - 标识符省略括号</li>
 *   <li>{@code foo() :: bar()} - 链式调用</li>
 *   <li>{@code obj :: { ... }} - 块上下文</li>
 *   <li>{@code obj ?:: transform()} - 安全上下文调用（null 短路）</li>
 *   <li>{@code obj ?:: { ... }} - 安全块上下文（null 短路）</li>
 * </ul>
 */
public class ContextCallInfixOperator implements InfixOperator {

    @Override
    public int bindingPower() {
        return 110;
    }

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.CONTEXT_CALL) || parser.check(TokenType.QUESTION_CONTEXT_CALL);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, ParseResult left, Token operator, Trampoline.Continuation<ParseResult> continuation) {
        // 判断是否为安全调用
        boolean safe = operator.is(TokenType.QUESTION_CONTEXT_CALL);
        // 处理省略括号的语法糖：time :: foo() 等价于 time() :: foo()
        if (left instanceof Identifier) {
            left = FunctionCallParser.getFunctionCallExpression(parser, (Identifier) left, new ParseResult[0]);
        }
        ParseResult finalLeft = left;
        // 检查右侧是块还是函数调用
        if (parser.match(TokenType.LEFT_BRACE)) {
            // 右侧是块：expr :: { ... } 或 expr ?:: { ... }
            boolean isContextCall = parser.getSymbolEnvironment().isContextCall();
            parser.getSymbolEnvironment().setContextCall(true);
            return BlockParser.parse(parser, block -> {
                parser.getSymbolEnvironment().setContextCall(isContextCall);
                ParseResult combined = parser.attachSource(new ContextCallExpression(finalLeft, block, safe), operator);
                // 处理后缀操作（如索引访问 []），链式 :: 由 parseInfixLoop 自动处理
                return continuation.apply(PostfixParser.parsePostfixOperations(parser, combined));
            });
        } else {
            // 右侧是函数调用：expr :: foo(...) 或 expr ?:: foo(...)
            SymbolEnvironment env = parser.getSymbolEnvironment();
            boolean isContextCall = env.isContextCall();
            env.setContextCall(true);
            ParseResult context = FunctionCallParser.parse(parser);
            env.setContextCall(isContextCall);
            ParseResult combined = parser.attachSource(new ContextCallExpression(finalLeft, context, safe), operator);
            // 处理后缀操作（如索引访问 []），链式 :: 由 parseInfixLoop 自动处理
            return continuation.apply(PostfixParser.parsePostfixOperations(parser, combined));
        }
    }
}
