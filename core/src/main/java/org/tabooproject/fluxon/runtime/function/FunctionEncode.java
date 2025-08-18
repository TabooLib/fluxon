package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.java.Export;
import org.tabooproject.fluxon.runtime.java.ExportRegistry;
import org.tabooproject.fluxon.runtime.java.Optional;
import org.tabooproject.fluxon.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 编码/解码函数
 * @author sky
 */
public class FunctionEncode {

    public static class HashObject {

        public static final HashObject INSTANCE = new HashObject();

        @Export
        public String md5(String input) {
            return StringUtils.hash(input, "MD5");
        }

        @Export
        public String sha1(String input) {
            return StringUtils.hash(input, "SHA-1");
        }

        @Export
        public String sha256(String input) {
            return StringUtils.hash(input, "SHA-256");
        }
        
        @Export
        public String sha384(String input) {
            return StringUtils.hash(input, "SHA-384");
        }

        @Export
        public String sha512(String input) {
            return StringUtils.hash(input, "SHA-512");
        }
    }

    public static class Base64Object {

        public static final Base64Object INSTANCE = new Base64Object();

        @Export
        public String encode(String input, @Optional String charset) {
            if (charset == null || charset.isEmpty()) {
                charset = "UTF-8";
            }
            try {
                return Base64.getEncoder().encodeToString(input.getBytes(charset));
            } catch (Exception e) {
                return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
            }
        }

        @Export
        public String decode(String input, @Optional String charset) {
            if (charset == null || charset.isEmpty()) {
                charset = "UTF-8";
            }
            try {
                return new String(Base64.getDecoder().decode(input), charset);
            } catch (Exception e) {
                return new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
            }
        }
    }

    public static class UnicodeObject {

        public static final UnicodeObject INSTANCE = new UnicodeObject();

        @Export
        public String encode(String input) {
            return StringUtils.unicodeEncode(input);
        }

        @Export
        public String decode(String input) {
            return StringUtils.unicodeDecode(input);
        }
    }

    public static class HexObject {

        public static final HexObject INSTANCE = new HexObject();

        @Export
        public String encode(String input) {
            return StringUtils.bytesToHex(input.getBytes(StandardCharsets.UTF_8));
        }

        @Export
        public String decode(String input) {
            return new String(StringUtils.hexToBytes(input), StandardCharsets.UTF_8);
        }
    }

    public static void init(FluxonRuntime runtime) {
        // 获取编码对象
        runtime.registerFunction("hash", 0, (context) -> HashObject.INSTANCE);
        runtime.registerFunction("base64", 0, (context) -> Base64Object.INSTANCE);
        runtime.registerFunction("unicode", 0, (context) -> UnicodeObject.INSTANCE);
        runtime.registerFunction("hex", 0, (context) -> HexObject.INSTANCE);
        
        // 注册编码相关的对象实例
        ExportRegistry exportRegistry = runtime.getExportRegistry();
        exportRegistry.registerClass(HashObject.class);
        exportRegistry.registerClass(Base64Object.class);
        exportRegistry.registerClass(UnicodeObject.class);
        exportRegistry.registerClass(HexObject.class);
    }
}