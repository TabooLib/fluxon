package org.tabooproject.fluxon.parser.type;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.Parser;

public class ImportParser {

    public static void parse(Parser parser) {
        while (parser.match(TokenType.IMPORT)) {
            // 检查是否禁用导入
            if (!parser.getContext().isAllowImport()) {
                parser.error("Import is not allowed");
                return;
            }
            // 获取导入名称
            String name;
            if (parser.check(TokenType.STRING) || parser.check(TokenType.IDENTIFIER)) {
                name = parser.consume().getLexeme();
            } else {
                parser.error("Expected import name");
                return;
            }
            // 检查是否在黑名单中
            if (parser.getContext().getPackageBlacklist().contains(name)) {
                parser.error("Importing package '" + name + "' is not allowed");
            }
            parser.getImports().add(name);
            parser.match(TokenType.SEMICOLON); // 可选的分号
        }
    }
}
