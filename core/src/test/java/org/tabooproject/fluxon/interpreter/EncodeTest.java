package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.compiler.FluxonFeatures;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 编码/解码函数测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class EncodeTest {

    @BeforeEach
    public void BeforeEach() {
        FluxonFeatures.DEFAULT_PACKAGE_AUTO_IMPORT.add("fs:crypto");
    }

    // ========== Base64 编码/解码测试 ==========

    @Test
    public void testBase64EncodeDecode() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = 'Hello, World!'; " +
                "encoded = base64::encode(&text); " +
                "decoded = base64::decode(&encoded); " +
                "&decoded");
        assertEquals("Hello, World!", result.getInterpretResult());
        assertEquals("Hello, World!", result.getCompileResult());
    }

    @Test
    public void testBase64EncodeResult() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "base64::encode('Hello, World!')");
        assertEquals("SGVsbG8sIFdvcmxkIQ==", result.getInterpretResult());
        assertEquals("SGVsbG8sIFdvcmxkIQ==", result.getCompileResult());
    }

    @Test
    public void testBase64WithSpecialChars() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = '你好，世界！'; " +
                "encoded = base64::encode(&text); " +
                "decoded = base64::decode(&encoded); " +
                "&decoded");
        assertEquals("你好，世界！", result.getInterpretResult());
        assertEquals("你好，世界！", result.getCompileResult());
    }

    @Test
    public void testBase64InvalidInput() {
        try {
            FluxonTestUtil.runSilent("base64::decode('Invalid!@#$Base64')");
            fail("Should throw exception for invalid Base64");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Illegal base64 character"));
        }
    }

    @Test
    public void testBase64MultiParams() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("base64::encode('Hello, World!', 'UTF-8')");
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());

        result = FluxonTestUtil.runSilent("base64::encode('Hello, World!', 'GBK')");
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());
    }

    @Test
    public void testBase64EmptyString() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("base64::encode('')");
        assertEquals("", result.getInterpretResult());
        assertEquals("", result.getCompileResult());

        result = FluxonTestUtil.runSilent("base64::decode('')");
        assertEquals("", result.getInterpretResult());
        assertEquals("", result.getCompileResult());
    }

    // ========== MD5 哈希测试 ==========

    @Test
    public void testMd5Hash() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("hash::md5('Hello')");
        assertEquals("8b1a9953c4611296a827abf8c47804d7", result.getInterpretResult());
        assertEquals("8b1a9953c4611296a827abf8c47804d7", result.getCompileResult());

        // 测试空字符串
        result = FluxonTestUtil.runSilent("hash::md5('')");
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result.getInterpretResult());
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result.getCompileResult());
    }

    @Test
    public void testMd5WithUnicode() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("hash::md5('你好')");
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult());
    }

    // ========== SHA 哈希测试 ==========

    @Test
    public void testSha1Hash() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("hash::sha1('Hello')");
        assertEquals("f7ff9e8b7bb2e09b70935a5d785e0cc5d9d0abf0", result.getInterpretResult());
        assertEquals("f7ff9e8b7bb2e09b70935a5d785e0cc5d9d0abf0", result.getCompileResult());
    }

    @Test
    public void testSha256Hash() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("hash::sha256('Hello')");
        assertEquals("185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969", result.getInterpretResult());
        assertEquals("185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969", result.getCompileResult());
    }

    @Test
    public void testSha384Hash() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("hash::sha384('Hello')");
        assertEquals("3519fe5ad2c596efe3e276a6f351b8fc0b03db861782490d45f7598ebd0ab5fd5520ed102f38c4a5ec834e98668035fc",
                result.getInterpretResult());
        assertEquals("3519fe5ad2c596efe3e276a6f351b8fc0b03db861782490d45f7598ebd0ab5fd5520ed102f38c4a5ec834e98668035fc",
                result.getCompileResult());
    }

    @Test
    public void testSha512Hash() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("hash::sha512('Hello')");
        assertEquals("3615f80c9d293ed7402687f94b22d58e529b8cc7916f8fac7fddf7fbd5af4cf777d3d795a7a00a16bf7e7f3fb9561ee9baae480da9fe7a18769e71886b03f315",
                result.getInterpretResult());
        assertEquals("3615f80c9d293ed7402687f94b22d58e529b8cc7916f8fac7fddf7fbd5af4cf777d3d795a7a00a16bf7e7f3fb9561ee9baae480da9fe7a18769e71886b03f315",
                result.getCompileResult());
    }

    @Test
    public void testHashConsistency() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = 'Test String'; " +
                "md5_1 = hash::md5(&text); " +
                "md5_2 = hash::md5(&text); " +
                "&md5_1 == &md5_2");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    // ========== 十六进制编码/解码测试 ==========

    @Test
    public void testHexEncodeDecode() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = 'Hello'; " +
                "encoded = hex::encode(&text); " +
                "decoded = hex::decode(&encoded); " +
                "&decoded");
        assertEquals("Hello", result.getInterpretResult());
        assertEquals("Hello", result.getCompileResult());
    }

    @Test
    public void testHexEncodeResult() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("hex::encode('Hello')");
        assertEquals("48656c6c6f", result.getInterpretResult());
        assertEquals("48656c6c6f", result.getCompileResult());
    }

    @Test
    public void testHexWithUnicode() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("hex::encode('你好')");
        assertEquals("e4bda0e5a5bd", result.getInterpretResult());
        assertEquals("e4bda0e5a5bd", result.getCompileResult());

        result = FluxonTestUtil.runSilent("hex::decode('e4bda0e5a5bd')");
        assertEquals("你好", result.getInterpretResult());
        assertEquals("你好", result.getCompileResult());
    }

    @Test
    public void testHexInvalidInput() {
        // 测试无效的十六进制输入
        try {
            FluxonTestUtil.runSilent("hex::decode('GG')");
            fail("Should throw exception for invalid hex");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("For input string"));
        }
        
        // 测试奇数长度的十六进制
        try {
            FluxonTestUtil.runSilent("hex::decode('48656c6c6')");
            fail("Should throw exception for odd length hex");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Hex string must have even length"));
        }
    }

    @Test
    public void testHexEmptyString() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("hex::encode('')");
        assertEquals("", result.getInterpretResult());
        assertEquals("", result.getCompileResult());

        result = FluxonTestUtil.runSilent("hex::decode('')");
        assertEquals("", result.getInterpretResult());
        assertEquals("", result.getCompileResult());
    }

    // ========== Unicode 编码/解码测试 ==========

    @Test
    public void testUnicodeEncodeDecode() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("unicode::encode('Hello 你好')");
        assertEquals("Hello \\u4f60\\u597d", result.getInterpretResult());
        assertEquals("Hello \\u4f60\\u597d", result.getCompileResult());

        result = FluxonTestUtil.runSilent("unicode::decode('Hello \\\\u4f60\\\\u597d')");
        assertEquals("Hello 你好", result.getInterpretResult());
        assertEquals("Hello 你好", result.getCompileResult());
    }

    @Test
    public void testUnicodeComplexString() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = '测试 Test 🚀 emoji'; " +
                "encoded = unicode::encode(&text); " +
                "decoded = unicode::decode(&encoded); " +
                "&decoded");
        assertEquals("测试 Test 🚀 emoji", result.getInterpretResult());
        assertEquals("测试 Test 🚀 emoji", result.getCompileResult());
    }

    @Test
    public void testUnicodePartialEscape() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "unicode::decode('Hello \\\\u4f60 World \\\\u597d!')");
        assertEquals("Hello 你 World 好!", result.getInterpretResult());
        assertEquals("Hello 你 World 好!", result.getCompileResult());
    }

    @Test
    public void testUnicodeInvalidEscape() {
        FluxonTestUtil.TestResult result;

        // 测试无效的十六进制
        result = FluxonTestUtil.runSilent("unicode::decode('\\\\uGGGG')");
        assertEquals("\\uGGGG", result.getInterpretResult());
        assertEquals("\\uGGGG", result.getCompileResult());

        // 测试不足4位
        result = FluxonTestUtil.runSilent("unicode::decode('\\\\u123')");
        assertEquals("\\u123", result.getInterpretResult());
        assertEquals("\\u123", result.getCompileResult());
    }

    @Test
    public void testUnicodeEmptyString() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("unicode::encode('')");
        assertEquals("", result.getInterpretResult());
        assertEquals("", result.getCompileResult());

        result = FluxonTestUtil.runSilent("unicode::decode('')");
        assertEquals("", result.getInterpretResult());
        assertEquals("", result.getCompileResult());
    }

    // ========== 链式编码测试 ==========

    @Test
    public void testChainedEncoding() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = 'Hello'; " +
                "step1 = base64::encode(&text); " +
                "step2 = hex::encode(&step1); " +
                "step3 = hex::decode(&step2); " +
                "step4 = base64::decode(&step3); " +
                "&step4");
        assertEquals("Hello", result.getInterpretResult());
        assertEquals("Hello", result.getCompileResult());
    }

    @Test
    public void testMultipleEncodingFormats() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = 'Test'; " +
                "b64 = base64::encode(&text); " +
                "hex = hex::encode(&text); " +
                "uni = unicode::encode(&text); " +
                "[&b64, &hex, &uni]");
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());
        assertEquals(result.getInterpretResult().toString(), result.getCompileResult().toString());
    }

    @Test
    public void testEncodingInLoop() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "results = []; " +
                "texts = ['a', 'b', 'c']; " +
                "for text in &texts { " +
                "  &results += base64::encode(&text)" +
                "}; " +
                "&results");
        assertEquals("[YQ==, Yg==, Yw==]", result.getInterpretResult().toString());
        assertEquals("[YQ==, Yg==, Yw==]", result.getCompileResult().toString());
    }

    // ========== 条件表达式中的编码测试 ==========

    @Test
    public void testEncodingInConditional() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = 'Hello'; " +
                "encoded = base64::encode(&text); " +
                "if &encoded::length() > 5 then 'long' else 'short'");
        assertEquals("long", result.getInterpretResult());
        assertEquals("long", result.getCompileResult());
    }

    @Test
    public void testHashComparison() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text1 = 'Hello'; " +
                "text2 = 'Hello'; " +
                "hash::md5(&text1) == hash::md5(&text2)");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    // ========== 上下文调用测试 ==========

    @Test
    public void testEncodingWithContextCall() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("text = 'Hello'; &text::uppercase()::length()");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());

        result = FluxonTestUtil.runSilent(
                "encoded = base64::encode('Hello'); " +
                "&encoded::substring(0, 4)");
        assertEquals("SGVs", result.getInterpretResult());
        assertEquals("SGVs", result.getCompileResult());
    }

    @Test
    public void testHashWithContextCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "hash::md5('Hello')::uppercase()");
        assertEquals("8B1A9953C4611296A827ABF8C47804D7", result.getInterpretResult());
        assertEquals("8B1A9953C4611296A827ABF8C47804D7", result.getCompileResult());
    }

    // ========== 边界情况和性能测试 ==========

    @Test
    public void testLongString() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("Hello");
        }
        
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = '" + longText + "'; " +
                "encoded = base64::encode(&text); " +
                "decoded = base64::decode(&encoded); " +
                "&decoded == &text");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testSpecialCharacters() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = '!@#$%^&*()_+-={}[]|:;<>?,./~`'; " +
                "encoded = base64::encode(&text); " +
                "decoded = base64::decode(&encoded); " +
                "&decoded");
        assertEquals("!@#$%^&*()_+-={}[]|:;<>?,./~`", result.getInterpretResult());
        assertEquals("!@#$%^&*()_+-={}[]|:;<>?,./~`", result.getCompileResult());
    }

    @Test
    public void testMultilineString() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = 'Line1\\nLine2\\nLine3'; " +
                "encoded = base64::encode(&text); " +
                "decoded = base64::decode(&encoded); " +
                "&decoded");
        assertEquals("Line1\nLine2\nLine3", result.getInterpretResult());
        assertEquals("Line1\nLine2\nLine3", result.getCompileResult());
    }

    @Test
    public void testAllHashAlgorithms() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "text = 'Test'; " +
                "md5 = hash::md5(&text); " +
                "sha1 = hash::sha1(&text); " +
                "sha256 = hash::sha256(&text); " +
                "[&md5::length(), &sha1::length(), &sha256::length()]");
        assertEquals("[32, 40, 64]", result.getInterpretResult().toString());
        assertEquals("[32, 40, 64]", result.getCompileResult().toString());
    }

    @Test
    public void testEncodingWithVariableReference() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "source = 'Hello'; " +
                "target = &source; " +
                "encoded = base64::encode(&target); " +
                "&encoded");
        assertEquals("SGVsbG8=", result.getInterpretResult());
        assertEquals("SGVsbG8=", result.getCompileResult());
    }

    @Test
    public void testNestedEncodingCalls() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "base64::decode(base64::encode('Test'))");
        assertEquals("Test", result.getInterpretResult());
        assertEquals("Test", result.getCompileResult());
    }
}

