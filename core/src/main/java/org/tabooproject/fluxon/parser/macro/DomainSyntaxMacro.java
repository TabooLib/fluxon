package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.DomainExpression;
import org.tabooproject.fluxon.parser.type.BlockParser;
import org.tabooproject.fluxon.parser.type.ExpressionParser;

/**
 * Domain 语法宏
 * <p>
 * 匹配已注册的 domain 标识符，解析域语法：{@code domain { ... }} 或 {@code domain expr}
 * <p>
 * 域语法提供了一种上下文封装机制，允许开发者注册自定义域来控制块或表达式的执行时机和方式。
 * 与 Command 不同，Domain 接收闭包（AST）作为参数，可以：
 * <ul>
 *   <li>完全控制块或表达式的执行时机和方式</li>
 *   <li>等待块执行完成并处理返回值</li>
 *   <li>多次执行块（如重试逻辑）</li>
 *   <li>通过 {@code Environment.target} 传递上下文</li>
 * </ul>
 *
 * <h3>语法示例</h3>
 * <pre>{@code
 * // 域 + 块
 * async {
 *     sleep(100)
 *     result = fetch("http://api.example.com")
 *     &result
 * }
 *
 * // 域 + 表达式
 * async sleep(100)
 *
 * // 结构化并发（通过 target 传递上下文）
 * scope {
 *     task1 = async { longOperation1() }
 *     task2 = async { longOperation2() }
 * }
 * }</pre>
 *
 * @see DomainRegistry
 * @see DomainExecutor
 * @see DomainExpression
 */
public class DomainSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        if (!parser.check(TokenType.IDENTIFIER)) {
            return false;
        }
        String name = parser.peek().getLexeme();
        DomainRegistry registry = parser.getContext().getDomainRegistry();
        return registry.hasDomain(name);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token domainToken = parser.consume();
        String name = domainToken.getLexeme();
        DomainRegistry registry = parser.getContext().getDomainRegistry();
        DomainExecutor executor = registry.get(name);
        // 检查是否为块语法: domain { ... }
        if (parser.check(TokenType.LEFT_BRACE)) {
            // 消费 '{' 然后解析块
            parser.consume();
            ParseResult body = BlockParser.parse(parser);
            DomainExpression domainExpr = new DomainExpression(name, body, executor);
            return continuation.apply(parser.attachSource(domainExpr, domainToken));
        } else {
            // 解析表达式: domain expr
            return ExpressionParser.parse(parser, body -> {
                DomainExpression domainExpr = new DomainExpression(name, body, executor);
                return continuation.apply(parser.attachSource(domainExpr, domainToken));
            });
        }
    }

    @Override
    public int priority() {
        // 高于普通标识符，与 Command 相同优先级
        return 1000;
    }
}
