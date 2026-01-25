package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.java.Export;
import org.tabooproject.fluxon.runtime.java.ExportRegistry;
import org.tabooproject.fluxon.runtime.java.Optional;
import org.tabooproject.fluxon.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

/**
 * 密码学和编码工具集
 *
 * @author sky
 */
public class FunctionCrypto {

    public static void init(FluxonRuntime runtime) {
        runtime.registerFunction("fs:crypto", "hash", returns(HashObject.TYPE).noParams(), context -> context.setReturnRef(HashObject.INSTANCE));
        runtime.registerFunction("fs:crypto", "base64", returns(Base64Object.TYPE).noParams(), context -> context.setReturnRef(Base64Object.INSTANCE));
        runtime.registerFunction("fs:crypto", "unicode", returns(UnicodeObject.TYPE).noParams(), context -> context.setReturnRef(UnicodeObject.INSTANCE));
        runtime.registerFunction("fs:crypto", "hex", returns(HexObject.TYPE).noParams(), context -> context.setReturnRef(HexObject.INSTANCE));

        ExportRegistry exportRegistry = runtime.getExportRegistry();
        exportRegistry.registerClass(HashObject.class, "fs:crypto");
        exportRegistry.registerClass(Base64Object.class, "fs:crypto");
        exportRegistry.registerClass(UnicodeObject.class, "fs:crypto");
        exportRegistry.registerClass(HexObject.class, "fs:crypto");
    }

    public static class HashObject {

        public static final HashObject INSTANCE = new HashObject();
        public static final Type TYPE = new Type(HashObject.class);

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
        public static final Type TYPE = new Type(Base64Object.class);

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
        public static final Type TYPE = new Type(UnicodeObject.class);

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
        public static final Type TYPE = new Type(HexObject.class);

        @Export
        public String encode(String input) {
            return StringUtils.bytesToHex(input.getBytes(StandardCharsets.UTF_8));
        }

        @Export
        public String decode(String input) {
            return new String(StringUtils.hexToBytes(input), StandardCharsets.UTF_8);
        }
    }
}
