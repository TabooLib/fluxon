package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.NewExpression;
import org.tabooproject.fluxon.parser.type.PostfixParser;

import static org.tabooproject.fluxon.parser.macro.SyntaxMacroHelper.parseArgumentList;
import static org.tabooproject.fluxon.parser.macro.SyntaxMacroHelper.parseQualifiedName;

/**
 * new 表达式语法宏
 * <p>
 * 语法: new fully.qualified.ClassName(args)
 * <p>
 * 语法示例：
 * <ul>
 *   <li>{@code new java.util.ArrayList()} - 无参构造</li>
 *   <li>{@code new java.util.ArrayList(10)} - 带参构造</li>
 *   <li>{@code new java.lang.StringBuilder("hello")} - 字符串参数</li>
 *   <li>{@code new java.util.ArrayList()::size()} - 链式调用</li>
 * </ul>
 */
public class NewSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.NEW);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token newToken = parser.consume(TokenType.NEW, "Expected 'new'");
        // 检查 Java 构造特性是否启用
        if (!parser.getContext().isAllowJavaConstruction()) {
            parser.error("Java object construction is not enabled. Use ctx.setAllowJavaConstruction(true) to enable the 'new' keyword.");
        }
        // 解析全限定类名
        String className = parseQualifiedName(parser, "Expected class name after 'new'");
        // 解析参数列表
        parser.consume(TokenType.LEFT_PAREN, "Expected '(' after class name");
        // 创建表达式并附加源信息
        ParseResult result = new NewExpression(className, parseArgumentList(parser));
        result = parser.attachSource(result, newToken);
        // 处理后缀操作（如索引访问 []、成员访问 . 和上下文调用 ::）
        return continuation.apply(PostfixParser.parsePostfixOperations(parser, result));
    }

    @Override
    public int priority() {
        return 100; // 与其他控制流关键字相同优先级
    }
}
