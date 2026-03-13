package org.tabooproject.fluxon.lexer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.compiler.CompilationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 进制数字字面量测试
 * 测试十六进制、二进制、八进制字面量的词法分析和端到端求值
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class NumericLiteralTest {

    /**
     * 对源代码进行词法分析，返回 Token 列表
     */
    private List<Token> tokenize(String source) {
        CompilationContext ctx = new CompilationContext(source);
        return new Lexer().process(ctx);
    }

    @Test
    public void testHexLiteral() {
        List<Token> tokens = tokenize("0xFF");
        assertEquals(TokenType.INTEGER, tokens.get(0).getType());
        assertEquals(255, tokens.get(0).getValue());
    }

    @Test
    public void testHexLiteralLowercase() {
        List<Token> tokens = tokenize("0xff");
        assertEquals(TokenType.INTEGER, tokens.get(0).getType());
        assertEquals(255, tokens.get(0).getValue());
    }

    @Test
    public void testHexLiteralUppercasePrefix() {
        List<Token> tokens = tokenize("0XFF");
        assertEquals(TokenType.INTEGER, tokens.get(0).getType());
        assertEquals(255, tokens.get(0).getValue());
    }

    @Test
    public void testBinaryLiteral() {
        List<Token> tokens = tokenize("0b1010");
        assertEquals(TokenType.INTEGER, tokens.get(0).getType());
        assertEquals(10, tokens.get(0).getValue());
    }

    @Test
    public void testBinaryLiteralWithUnderscores() {
        List<Token> tokens = tokenize("0B1111_0000");
        assertEquals(TokenType.INTEGER, tokens.get(0).getType());
        assertEquals(240, tokens.get(0).getValue());
    }

    @Test
    public void testOctalLiteral() {
        List<Token> tokens = tokenize("0o777");
        assertEquals(TokenType.INTEGER, tokens.get(0).getType());
        assertEquals(511, tokens.get(0).getValue());
    }

    @Test
    public void testHexLongSuffix() {
        List<Token> tokens = tokenize("0xFFL");
        assertEquals(TokenType.LONG, tokens.get(0).getType());
        assertEquals(255L, tokens.get(0).getValue());
    }

    @Test
    public void testBinaryLongSuffix() {
        List<Token> tokens = tokenize("0b1L");
        assertEquals(TokenType.LONG, tokens.get(0).getType());
        assertEquals(1L, tokens.get(0).getValue());
    }

    @Test
    public void testHexOverflowToLong() {
        // 0xFFFFFFFF 超出 int 范围，自动升级为 long
        List<Token> tokens = tokenize("0xFFFFFFFF");
        assertEquals(TokenType.LONG, tokens.get(0).getType());
        assertEquals(4294967295L, tokens.get(0).getValue());
    }

    @Test
    public void testHexZero() {
        List<Token> tokens = tokenize("0x0");
        assertEquals(TokenType.INTEGER, tokens.get(0).getType());
        assertEquals(0, tokens.get(0).getValue());
    }

    @Test
    public void testHexAddition() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("0xFF + 1");
        assertEquals(256, result.getInterpretResult());
        assertEquals(256, result.getCompileResult());
    }

    @Test
    public void testBinaryEquality() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("0b1010 == 10");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testOctalEquality() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("0o10 == 8");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }
}
