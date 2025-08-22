package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.lexer.Lexer;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.expression.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals("[fs]", parseSource("import fs").toString());
        assertEquals("[fs, fs:crypto]", parseSource("import fs; import 'fs:crypto'").toString());
    }
}