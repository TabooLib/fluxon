package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.interpreter.error.FunctionNotFoundException;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;

import java.util.*;

public class FunctionCallParser {

    /**
     * 解析函数调用表达式
     *
     * @return 函数调用表达式解析结果
     */
    public static ParseResult parse(Parser parser) {
        ParseResult name = ExpressionParser.parsePrimary(parser);
        if (name instanceof Identifier) {
            // 有括号
            if (parser.match(TokenType.LEFT_PAREN)) {
                return finishCall(parser, (Identifier) name);
            }
            // 一种特殊写法：
            // 允许 time :: now() 这种无参数顶层函数调用，类似于静态工具
            else if (parser.check(TokenType.CONTEXT_CALL)) {
                name = finishTopLevelContextCall(parser, (Identifier) name);
            }
        }
        // 处理后缀操作：函数调用 () 和索引访问 []
        return PostfixParser.parsePostfixOperations(parser, name);
    }

    public static ParseResult finishCall(Parser parser, Identifier callee) {
        List<ParseResult> arguments = new ArrayList<>();
        // 如果参数列表不为空
        if (!parser.check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(ExpressionParser.parse(parser));
            } while (parser.match(TokenType.COMMA) && !parser.check(TokenType.RIGHT_PAREN));
        }
        parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
        return getFunctionCallExpression(parser, callee, arguments.toArray(new ParseResult[0]));
    }

    private static ParseResult finishTopLevelContextCall(Parser parser, Identifier callee) {
        return getFunctionCallExpression(parser, callee, new ParseResult[0]);
    }

    private static FunctionCallExpression getFunctionCallExpression(Parser parser, Identifier callee, ParseResult[] arguments) {
        String name = callee.getValue();
        // 先尝试查找函数信息（普通函数和扩展函数）
        FunctionInfo funcInfo = FunctionInfo.lookup(parser, name);
        // 如果找到了函数，直接返回
        if (funcInfo.isFound()) {
            return new FunctionCallExpression(name, arguments, funcInfo.getPosition(), funcInfo.getExtensionPosition());
        }
        // 如果函数不存在，创建未解析的调用表达式并注册到待解析列表
        // 这样可以支持前向引用（函数定义在调用之后）
        FunctionCallExpression expression = new FunctionCallExpression(name, arguments, null, null);
        parser.registerPendingCall(expression, parser.previous());
        return expression;
    }
}
