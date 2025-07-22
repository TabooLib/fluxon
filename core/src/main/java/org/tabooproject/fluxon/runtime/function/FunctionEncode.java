package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 编码/解码函数
 * @author sky
 */
public class FunctionEncode {

    public static final HashObject HASH_OBJECT = new HashObject();

    public static final Base64Object BASE64_OBJECT = new Base64Object();

    public static final UnicodeObject UNICODE_OBJECT = new UnicodeObject();

    public static final HexObject HEX_OBJECT = new HexObject();

    public static class HashObject {}

    public static class Base64Object {}

    public static class UnicodeObject {}

    public static class HexObject {}

    public static void init(FluxonRuntime runtime) {
        // 获取编码对象
        runtime.registerFunction("hash", 0, (context) -> HASH_OBJECT);
        runtime.registerFunction("base64", 0, (context) -> BASE64_OBJECT);
        runtime.registerFunction("unicode", 0, (context) -> UNICODE_OBJECT);
        runtime.registerFunction("hex", 0, (context) -> HEX_OBJECT);

        // MD5 哈希
        runtime.registerExtensionFunction(HashObject.class, "md5", 1, (context) -> {
            Object[] args = context.getArguments();
            return StringUtils.hash(args[0].toString(), "MD5");
        });
        // SHA-1 哈希
        runtime.registerExtensionFunction(HashObject.class, "sha1", 1, (context) -> {
            Object[] args = context.getArguments();
            return StringUtils.hash(args[0].toString(), "SHA-1");
        });
        // SHA-256 哈希
        runtime.registerExtensionFunction(HashObject.class, "sha256", 1, (context) -> {
            Object[] args = context.getArguments();
            return StringUtils.hash(args[0].toString(), "SHA-256");
        });
        // SHA-384 哈希
        runtime.registerExtensionFunction(HashObject.class, "sha384", 1, (context) -> {
            Object[] args = context.getArguments();
            return StringUtils.hash(args[0].toString(), "SHA-384");
        });
        // SHA-512 哈希
        runtime.registerExtensionFunction(HashObject.class, "sha512", 1, (context) -> {
            Object[] args = context.getArguments();
            return StringUtils.hash(args[0].toString(), "SHA-512");
        });

        // Base64 编码
        runtime.registerExtensionFunction(Base64Object.class, "encode", 1, (context) -> {
            Object[] args = context.getArguments();
            return Base64.getEncoder().encodeToString(args[0].toString().getBytes(StandardCharsets.UTF_8));
        });
        // Base64 解码
        runtime.registerExtensionFunction(Base64Object.class, "decode", 1, (context) -> {
            Object[] args = context.getArguments();
            try {
                byte[] decoded = Base64.getDecoder().decode(args[0].toString());
                return new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid Base64 input: " + e.getMessage());
            }
        });

        // 十六进制编码
        runtime.registerExtensionFunction(HexObject.class, "encode", 1, (context) -> {
            Object[] args = context.getArguments();
            return StringUtils.bytesToHex(args[0].toString().getBytes(StandardCharsets.UTF_8));
        });
        // 十六进制解码
        runtime.registerExtensionFunction(HexObject.class, "decode", 1, (context) -> {
            Object[] args = context.getArguments();
            try {
                return new String(StringUtils.hexToBytes(args[0].toString()), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid hex input: " + e.getMessage());
            }
        });

        // Unicode 转义编码
        runtime.registerExtensionFunction(UnicodeObject.class, "encode", 1, (context) -> {
            Object[] args = context.getArguments();
            return StringUtils.unicodeEncode(args[0].toString());
        });
        // Unicode 转义解码
        runtime.registerExtensionFunction(UnicodeObject.class, "decode", 1, (context) -> {
            Object[] args = context.getArguments();
            return StringUtils.unicodeDecode(args[0].toString());
        });
    }
}