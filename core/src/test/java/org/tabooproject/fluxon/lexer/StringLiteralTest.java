package org.tabooproject.fluxon.lexer;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * raw 字符串和多行字符串测试
 *
 * @author sky
 */
public class StringLiteralTest {

    @Test
    public void testRawStringNoEscape() {
        // raw 字符串中反斜杠不转义
        Object result = eval("r\"hello\\nworld\"");
        assertEquals("hello\\nworld", result);
    }

    @Test
    public void testRawStringSingleQuote() {
        Object result = eval("r'hello\\tworld'");
        assertEquals("hello\\tworld", result);
    }

    @Test
    public void testMultilineString() {
        Object result = eval("\"\"\"line1\nline2\nline3\"\"\"");
        assertEquals("line1\nline2\nline3", result);
    }

    @Test
    public void testMultilineStringPreservesContent() {
        Object result = eval("\"\"\"hello world\"\"\"");
        assertEquals("hello world", result);
    }

    @Test
    public void testRawStringConcat() {
        Object result = eval("r\"C:\\Users\" + '\\\\' + r\"test\"");
        assertEquals("C:\\Users\\test", result);
    }

    private Object eval(String source) {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        return Fluxon.eval(source, env);
    }
}
