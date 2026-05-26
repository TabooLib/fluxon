package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.macro.SyntaxMacroHelper;

/**
 * 顶层类型别名解析器
 * 用于把脚本内短类型名绑定到具体 Java 类型，避免长全限定名污染函数签名。
 *
 * @author sky
 */
public class TypeAliasParser {

    public static void parse(Parser parser) {
        while (parser.match(TokenType.TYPEALIAS)) {
            parseBody(parser);
        }
    }

    public static void parseOne(Parser parser) {
        parser.consume(TokenType.TYPEALIAS, "Expected 'typealias'");
        parseBody(parser);
    }

    private static void parseBody(Parser parser) {
        Token aliasToken = parser.consume(TokenType.IDENTIFIER, "Expected type alias name");
        parser.consume(TokenType.ASSIGN, "Expected '=' after type alias name");
        Class<?> type = SyntaxMacroHelper.parseAndResolveType(parser);
        parser.getContext().defineTypeAlias(aliasToken.getLexeme(), type);
        parser.match(TokenType.SEMICOLON);
    }
}
