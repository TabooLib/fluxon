package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.expression.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImportParserTest {

    private List<String> parseSource(String source) {
        // 创建编译上下文
        CompilationContext context = new CompilationContext(source);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.process(context);
        context.setAttribute("tokens", tokens);
        Parser parser = new Parser();
        parser.process(context);
        return parser.getImports();
    }

    @Test
    public void testImport1() {
        assertTrue(parseSource("import fs").contains("fs"));
        List<String> imports = parseSource("import fs; import 'fs:crypto'");
        assertTrue(imports.contains("fs") && imports.contains("fs:crypto"));
    }
}