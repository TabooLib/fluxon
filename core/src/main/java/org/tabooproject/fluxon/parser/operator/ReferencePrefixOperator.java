package org.tabooproject.fluxon.parser.operator;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.error.VariableNotFoundException;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.type.PostfixParser;

import java.util.ArrayList;

/**
 * 引用前缀运算符 (&, &?)
 * <p>
 * 优先级: 100（最高，在其他前缀运算符之前处理）
 */
public class ReferencePrefixOperator implements PrefixOperator {

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.AMPERSAND);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        parser.consume(); // consume &
        boolean isOptional = parser.match(TokenType.QUESTION);
        String name = parser.consume(TokenType.IDENTIFIER, "Expect variable name after '&'.").getLexeme();

        ParseResult ref;
        if (isOptional) {
            ref = new ReferenceExpression(new Identifier(name), true, -1);
        } else {
            Integer captured = parser.getCapturedIndex(name);
            if (captured != null) {
                ref = new ReferenceExpression(new Identifier(name), false, captured);
            } else if (parser.getContext().isAllowInvalidReference() || parser.isFunction(name) || parser.hasVariable(name)) {
                ref = new ReferenceExpression(new Identifier(name), parser.getContext().isAllowInvalidReference(),
                        parser.getSymbolEnvironment().getLocalVariable(name));
            } else {
                Token token = parser.peek();
                SourceExcerpt excerpt = SourceExcerpt.from(parser.getContext(), token);
                throw new VariableNotFoundException(name,
                        new ArrayList<>(parser.getSymbolEnvironment().getLocalVariables().keySet()), token, excerpt);
            }
        }
        // 处理后缀操作（函数调用、数组访问等）
        ref = PostfixParser.parsePostfixOperations(parser, ref);
        // 中缀运算符（包括 ::）会在 parseInfixLoop 中自动处理
        return continuation.apply(ref);
    }
}
