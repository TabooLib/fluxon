package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.expression.StaticAccessExpression;
import org.tabooproject.fluxon.parser.type.PostfixParser;

import static org.tabooproject.fluxon.parser.macro.SyntaxMacroHelper.parseArgumentList;
import static org.tabooproject.fluxon.parser.macro.SyntaxMacroHelper.parseQualifiedName;

/**
 * static 表达式语法宏
 * <p>
 * 语法: static fully.qualified.ClassName.member 或 static fully.qualified.ClassName.method(args)
 * <p>
 * 语法示例：
 * <ul>
 *   <li>{@code static java.lang.Integer.parseInt("42")} - 静态方法调用</li>
 *   <li>{@code static java.lang.System.out} - 静态字段访问</li>
 *   <li>{@code static java.lang.System.out.println("hello")} - 链式调用</li>
 *   <li>{@code static java.lang.Math.PI} - 常量访问</li>
 * </ul>
 */
public class StaticSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.STATIC);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token staticToken = parser.consume(TokenType.STATIC, "Expected 'static'");
        // 检查 Java 静态访问特性是否启用（复用 Java 构造特性开关）
        if (!parser.getContext().isAllowJavaConstruction()) {
            parser.error("Java static access is not enabled. Use ctx.setAllowJavaConstruction(true) to enable the 'static' keyword.");
        }
        // 解析全限定路径（ClassName.memberName 格式）
        String fullPath = parseQualifiedName(parser, "Expected class name after 'static'");
        // 分离类名和成员名
        int lastDot = fullPath.lastIndexOf('.');
        if (lastDot == -1) {
            parser.error("Expected 'static ClassName.memberName', but got 'static " + fullPath + "'");
        }
        String className = fullPath.substring(0, lastDot);
        String memberName = fullPath.substring(lastDot + 1);
        // 检查是方法调用还是字段访问
        boolean isMethodCall = parser.check(TokenType.LEFT_PAREN);
        ParseResult[] args = new ParseResult[0];
        if (isMethodCall) {
            parser.consume(TokenType.LEFT_PAREN, "Expected '(' after method name");
            args = parseArgumentList(parser);
        }
        // 创建表达式并附加源信息
        ParseResult result = new StaticAccessExpression(className, memberName, args, isMethodCall);
        result = parser.attachSource(result, staticToken);
        // 处理后缀操作（如索引访问 []、成员访问 . 和上下文调用 ::）
        return continuation.apply(PostfixParser.parsePostfixOperations(parser, result));
    }

    @Override
    public int priority() {
        return 100; // 与其他控制流关键字相同优先级
    }
}
