package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 编码/解码函数测试
 *
 * @author sky
 */
public class EncodeTest {

    @Test
    public void testBase64EncodeDecode() {
        // 测试基本的 Base64 编码和解码
        Object result = Fluxon.eval("text = \"Hello, World!\"\n" +
                "encoded = base64::encode(&text)\n" +
                "decoded = base64::decode(&encoded)\n" +
                "return &decoded");
        assertEquals("Hello, World!", result);
        
        // 测试编码结果
        Object encoded = Fluxon.eval("base64::encode(\"Hello, World!\")");
        assertEquals("SGVsbG8sIFdvcmxkIQ==", encoded);
    }

    @Test
    public void testBase64WithSpecialChars() {
        // 测试包含特殊字符的 Base64 编码
        Object result = Fluxon.eval("text = \"你好，世界！\"\n" +
                "encoded = base64::encode(&text)\n" +
                "decoded = base64::decode(&encoded)\n" +
                "return &decoded");
        assertEquals("你好，世界！", result);
    }

    @Test
    public void testBase64InvalidInput() {
        // 测试无效的 Base64 输入
        try {
            Fluxon.eval("base64::decode(\"Invalid!@#$Base64\")");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Illegal base64 character"));
        }
    }

    @Test
    public void testBase64MultiParams() {
        Fluxon.eval("base64::encode(\"Hello, World!\", \"UTF-8\")");
        Fluxon.eval("base64::encode(\"Hello, World!\", \"GBK\")");
    }

    @Test
    public void testMd5Hash() {
        // 测试 MD5 哈希
        Object result = Fluxon.eval("hash::md5(\"Hello\")");
        assertEquals("8b1a9953c4611296a827abf8c47804d7", result);
        
        // 测试空字符串
        Object emptyResult = Fluxon.eval("hash::md5(\"\")");
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", emptyResult);
    }

    @Test
    public void testSha1Hash() {
        // 测试 SHA-1 哈希
        Object result = Fluxon.eval("hash::sha1(\"Hello\")");
        assertEquals("f7ff9e8b7bb2e09b70935a5d785e0cc5d9d0abf0", result);
    }

    @Test
    public void testSha256Hash() {
        // 测试 SHA-256 哈希
        Object result = Fluxon.eval("hash::sha256(\"Hello\")");
        assertEquals("185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969", result);
    }

    @Test
    public void testSha384Hash() {
        // 测试 SHA-384 哈希
        Object result = Fluxon.eval("hash::sha384(\"Hello\")");
        assertEquals("3519fe5ad2c596efe3e276a6f351b8fc0b03db861782490d45f7598ebd0ab5fd5520ed102f38c4a5ec834e98668035fc", result);
    }

    @Test
    public void testSha512Hash() {
        // 测试 SHA-512 哈希
        Object result = Fluxon.eval("hash::sha512(\"Hello\")");
        assertEquals("3615f80c9d293ed7402687f94b22d58e529b8cc7916f8fac7fddf7fbd5af4cf777d3d795a7a00a16bf7e7f3fb9561ee9baae480da9fe7a18769e71886b03f315", result);
    }

    @Test
    public void testHexEncodeDecode() {
        // 测试十六进制编码和解码
        Object result = Fluxon.eval("text = \"Hello\"\n" +
                "encoded = hex::encode(&text)\n" +
                "decoded = hex::decode(&encoded)\n" +
                "return &decoded");
        assertEquals("Hello", result);
        
        // 测试编码结果
        Object encoded = Fluxon.eval("hex::encode(\"Hello\")");
        assertEquals("48656c6c6f", encoded);
    }

    @Test
    public void testHexWithUnicode() {
        // 测试包含 Unicode 的十六进制编码
        Object encoded = Fluxon.eval("hex::encode(\"你好\")");
        assertEquals("e4bda0e5a5bd", encoded);
        
        Object decoded = Fluxon.eval("hex::decode(\"e4bda0e5a5bd\")");
        assertEquals("你好", decoded);
    }

    @Test
    public void testHexInvalidInput() {
        // 测试无效的十六进制输入
        try {
            Fluxon.eval("hex::decode(\"GG\")"); // GG 不是有效的十六进制
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("For input string"));
        }
        
        // 测试奇数长度的十六进制
        try {
            Fluxon.eval("hex::decode(\"48656c6c6\")"); // 奇数长度
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Hex string must have even length"));
        }
    }

    @Test
    public void testUnicodeEncodeDecode() {
        // 测试 Unicode 转义编码和解码
        Object encoded = Fluxon.eval("unicode::encode(\"Hello 你好\")");
        assertEquals("Hello \\u4f60\\u597d", encoded);
        
        Object decoded = Fluxon.eval("unicode::decode(\"Hello \\\\u4f60\\\\u597d\")");
        assertEquals("Hello 你好", decoded);
    }

    @Test
    public void testUnicodeComplexString() {
        // 测试复杂的 Unicode 字符串
        Object result = Fluxon.eval("text = \"测试 Test 🚀 emoji\"\n" +
                "encoded = unicode::encode(&text)\n" +
                "decoded = unicode::decode(&encoded)\n" +
                "return &decoded");
        assertEquals("测试 Test 🚀 emoji", result);
    }

    @Test
    public void testUnicodePartialEscape() {
        // 测试部分 Unicode 转义
        Object decoded = Fluxon.eval("unicode::decode(\"Hello \\\\u4f60 World \\\\u597d!\")");
        assertEquals("Hello 你 World 好!", decoded);
    }

    @Test
    public void testUnicodeInvalidEscape() {
        // 测试无效的 Unicode 转义序列
        Object result = Fluxon.eval("unicode::decode(\"\\\\uGGGG\")"); // 无效的十六进制
        assertEquals("\\uGGGG", result); // 应该保持原样
        
        result = Fluxon.eval("unicode::decode(\"\\\\u123\")"); // 不足4位
        assertEquals("\\u123", result); // 应该保持原样
    }

    @Test
    public void testChainedEncoding() {
        // 测试链式编码
        Object result = Fluxon.eval("text = \"Hello\"\n" +
                "// 先 Base64 编码，再十六进制编码\n" +
                "step1 = base64::encode(&text)\n" +
                "step2 = hex::encode(&step1)\n" +
                "// 反向解码\n" +
                "step3 = hex::decode(&step2)\n" +
                "step4 = base64::decode(&step3)\n" +
                "return &step4");
        assertEquals("Hello", result);
    }

    @Test
    public void testHashConsistency() {
        // 测试哈希的一致性
        String code = "text = \"Test String\"\n" +
                "md5_1 = hash::md5(&text)\n" +
                "md5_2 = hash::md5(&text)\n" +
                "return &md5_1 == &md5_2";
        Object result = Fluxon.eval(code);
        assertEquals(true, result);
    }

    @Test
    public void testEmptyStringEncoding() {
        // 测试空字符串的编码
        assertEquals("", Fluxon.eval("base64::encode(\"\")"));
        assertEquals("", Fluxon.eval("base64::decode(\"\")"));
        assertEquals("", Fluxon.eval("hex::encode(\"\")"));
        assertEquals("", Fluxon.eval("hex::decode(\"\")"));
        assertEquals("", Fluxon.eval("unicode::encode(\"\")"));
        assertEquals("", Fluxon.eval("unicode::decode(\"\")"));
    }

    @Test
    public void testPerformanceUnicodeDecode() {
        // 性能测试：大量 Unicode 转义的解码
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("\\u4e00\\u4e01\\u4e02\\u4e03\\u4e04");
        }
        
        long start = System.currentTimeMillis();
        Object result = Fluxon.eval("unicode::decode(\"" + sb + "\")");
        long end = System.currentTimeMillis();
        
        assertNotNull(result);
        assertTrue(end - start < 100, "Unicode decode should be fast");
    }
}