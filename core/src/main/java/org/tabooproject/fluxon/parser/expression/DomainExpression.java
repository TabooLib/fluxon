package org.tabooproject.fluxon.parser.expression;

import org.tabooproject.fluxon.parser.DomainExecutor;
import org.tabooproject.fluxon.parser.ParseResult;

/**
 * 域表达式
 * <p>
 * 表示形如 {@code domain { ... }} 或 {@code domain expr} 的域语法。
 * 域提供了比 {@code ::} 上下文调用更强大的控制流机制，允许执行器完全控制
 * 代码块的执行时机、方式和环境。
 * </p>
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
 * // 嵌套域
 * transaction {
 *     scope {
 *         async { operation1() }
 *         async { operation2() }
 *     }
 *     commit()
 * }
 * }</pre>
 *
 * @see org.tabooproject.fluxon.parser.DomainRegistry
 * @see org.tabooproject.fluxon.parser.DomainExecutor
 */
public class DomainExpression extends Expression {

    private final String domainName;
    private final ParseResult body;
    private final DomainExecutor executor;

    /**
     * 构造域表达式
     *
     * @param domainName 域名称（标识符），如 "async", "transaction"
     * @param body       域体（块或表达式的 AST）
     * @param executor   域执行器，负责控制域体的执行
     */
    public DomainExpression(String domainName, ParseResult body, DomainExecutor executor) {
        super(ExpressionType.DOMAIN);
        this.domainName = domainName;
        this.body = body;
        this.executor = executor;
    }

    /**
     * 获取域名称
     *
     * @return 域名称
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * 获取域体（未求值的 AST）
     *
     * @return 域体 ParseResult
     */
    public ParseResult getBody() {
        return body;
    }

    /**
     * 获取域执行器
     *
     * @return DomainExecutor 实例
     */
    public DomainExecutor getExecutor() {
        return executor;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.DOMAIN;
    }

    @Override
    public String toString() {
        return "DomainExpression{" +
                "domainName='" + domainName + '\'' +
                ", body=" + body +
                '}';
    }

    @Override
    public String toPseudoCode() {
        return domainName + " " + body.toPseudoCode();
    }
}
