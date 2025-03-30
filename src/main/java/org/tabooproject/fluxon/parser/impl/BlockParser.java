package org.tabooproject.fluxon.parser.impl;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.statements.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockParser {

    /**
     * 解析代码块
     * 代码块里包含多个语句（Statement）
     *
     * @param innerVars 内部变量
     * @return 代码块解析结果
     */
    public static ParseResult parse(Parser parser, List<String> innerVars) {
        // 进入新的作用域
        parser.enterScope();
        // 定义内部变量
        for (String innerVar : innerVars) {
            parser.defineVariable(innerVar);
        }

        // 解析 Statement
        List<ParseResult> statements = new ArrayList<>();
        while (!parser.check(TokenType.RIGHT_BRACE) && !parser.isAtEnd()) {
            statements.add(StatementParser.parse(parser));
        }
        
        // 消费右大括号
        parser.consume(TokenType.RIGHT_BRACE, "Expected '}' after block");
        // 退出当前作用域
        parser.exitScope();
        return new Block(null, statements);
    }
}
