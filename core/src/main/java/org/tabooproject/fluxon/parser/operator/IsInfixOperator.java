package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.IsExpression;
import org.tabooproject.fluxon.parser.macro.SyntaxMacroHelper;

/**
 * Is 类型检查运算符
 * <p>
 * 绑定力: 80，左结合（与比较运算符相同）
 * <p>
 * 语法：expression is TypeName
 * <p>
 * 右侧只能是类型字面量（标识符或点分限定名），不能是表达式。
 * 类型在解析期解析并缓存到 AST 节点中。
 */
public class IsInfixOperator implements InfixOperator {

    @Override
    public int bindingPower() {
        return 80;
    }

    @Override
    public boolean matches(Parser parser) {
        // 跨行时不作为中缀操作符匹配，避免 when 块中 "is" 开头的分支被误解析
        return parser.check(TokenType.IS) && !parser.isStatementBoundary();
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, ParseResult left, Token operator, Trampoline.Continuation<ParseResult> continuation) {
        // 解析类型字面量并解析为 Class 对象
        String typeLiteral = SyntaxMacroHelper.parseQualifiedName(parser, "Expected type name after 'is'").name;
        Class<?> targetClass = SyntaxMacroHelper.resolveTypeName(typeLiteral, parser);
        // 构建 IsExpression
        IsExpression isExpr = new IsExpression(left, operator, typeLiteral, targetClass);
        return continuation.apply(parser.attachSource(isExpr, operator));
    }
}
