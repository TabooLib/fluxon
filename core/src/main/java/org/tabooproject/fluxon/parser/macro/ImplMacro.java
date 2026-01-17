package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.SyntaxMacro;
import org.tabooproject.fluxon.parser.Trampoline;
import org.tabooproject.fluxon.parser.definition.MethodDefinition;
import org.tabooproject.fluxon.parser.expression.AnonymousClassExpression;
import org.tabooproject.fluxon.parser.type.BlockParser;
import org.tabooproject.fluxon.parser.type.ExpressionParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.tabooproject.fluxon.parser.macro.FunctionDefinitionMacro.parseParameters;
import static org.tabooproject.fluxon.parser.macro.SyntaxMacroHelper.parseQualifiedName;

/**
 * impl 表达式语法宏
 *
 * @author sky
 */
public class ImplMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        return parser.check(TokenType.IMPL);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token implToken = parser.consume(TokenType.IMPL, "Expected 'impl'");
        if (!parser.getContext().isAllowJavaConstruction()) {
            parser.error("Java object construction is not enabled");
        }
        parser.consume(TokenType.COLON, "Expected ':' after 'impl'");
        // 解析类型列表，第一个带括号的是父类
        String superClass = null;
        List<ParseResult> superArgs = null;
        List<String> interfaces = new ArrayList<>();
        do {
            String typeName = parseQualifiedName(parser, "Expected type name").name;
            if (parser.check(TokenType.LEFT_PAREN)) {
                parser.advance();
                superClass = typeName;
                superArgs = new ArrayList<>();
                if (!parser.check(TokenType.RIGHT_PAREN)) {
                    do {
                        superArgs.add(parser.parseExpression());
                    } while (parser.match(TokenType.COMMA));
                }
                parser.consume(TokenType.RIGHT_PAREN, "Expected ')'");
            } else {
                interfaces.add(typeName);
            }
        } while (parser.match(TokenType.COMMA) && !parser.check(TokenType.LEFT_BRACE));
        // 解析方法
        parser.consume(TokenType.LEFT_BRACE, "Expected '{'");
        List<MethodDefinition> methods = new ArrayList<>();
        while (parser.check(TokenType.OVERRIDE)) {
            methods.add(parseMethod(parser));
        }
        parser.consume(TokenType.RIGHT_BRACE, "Expected '}'");
        ParseResult result = new AnonymousClassExpression(superClass, superArgs, interfaces, methods);
        return continuation.apply(parser.attachSource(result, implToken));
    }

    private MethodDefinition parseMethod(Parser parser) {
        parser.consume(TokenType.OVERRIDE, "Expected 'override'");
        String methodName = parser.consume(TokenType.IDENTIFIER, "Expected method name").getLexeme();
        String scopeId = "$impl$" + methodName;
        parser.getSymbolEnvironment().setCurrentFunction(scopeId);
        List<String> paramNames = new ArrayList<>(parseParameters(parser, false, TokenType.ASSIGN, TokenType.LEFT_BRACE).keySet());
        ParseResult body;
        if (parser.match(TokenType.ASSIGN)) {
            body = ExpressionParser.parse(parser);
        } else if (parser.match(TokenType.LEFT_BRACE)) {
            body = BlockParser.parse(parser);
        } else {
            throw parser.createParseException("Expected '=' or '{'", parser.peek());
        }
        parser.match(TokenType.SEMICOLON);
        Set<String> locals = parser.getSymbolEnvironment().getLocalVariables().get(scopeId);
        parser.getSymbolEnvironment().setCurrentFunction(null);
        return new MethodDefinition(methodName, paramNames, null, body, locals != null ? locals : new HashSet<>());
    }
}
