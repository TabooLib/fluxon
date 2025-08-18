package org.tabooproject.fluxon.util;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 伪代码生成工具类
 */
public class StringUtils {

    /**
     * 生成指定数量的缩进空格
     *
     * @param indent 缩进级别
     * @return 缩进字符串
     */
    public static String getIndent(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("    ");
        }
        return sb.toString();
    }

    /**
     * 计算哈希值
     */
    public static String hash(String input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not supported: " + algorithm);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * 十六进制字符串转字节数组
     */
    public static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
        }
        return bytes;
    }

    /**
     * Unicode 转义编码
     */
    public static String unicodeEncode(String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c > 127) {
                result.append("\\u").append(String.format("%04x", (int) c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Unicode 转义解码 - 性能优化版本
     */
    public static String unicodeDecode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        int len = input.length();
        StringBuilder result = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 5 < len && input.charAt(i + 1) == 'u') {
                // 快速检查是否为有效的 Unicode 转义序列
                int j = i + 2;
                boolean isValid = true;
                for (int k = 0; k < 4; k++) {
                    char hex = input.charAt(j + k);
                    if (!((hex >= '0' && hex <= '9') || (hex >= 'a' && hex <= 'f') || (hex >= 'A' && hex <= 'F'))) {
                        isValid = false;
                        break;
                    }
                }
                if (isValid) {
                    // 手动解析十六进制，避免 substring 和 parseInt 的开销
                    int code = getCode(input, j);
                    result.append((char) code);
                    i += 5;
                } else {
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static int getCode(String input, int j) {
        int code = 0;
        for (int k = 0; k < 4; k++) {
            char hex = input.charAt(j + k);
            code <<= 4;
            if (hex >= '0' && hex <= '9') {
                code |= (hex - '0');
            } else if (hex >= 'a' && hex <= 'f') {
                code |= (hex - 'a' + 10);
            } else if (hex >= 'A' && hex <= 'F') {
                code |= (hex - 'A' + 10);
            }
        }
        return code;
    }

    /**
     * 转换方法名，自动去掉 get 前缀
     *
     * @param originalName 原始方法名
     * @return 转换后的方法名
     */
    public static String transformMethodName(String originalName) {
        // 如果方法名以 "get" 开头且后面有内容
        if (originalName.startsWith("get") && originalName.length() > 3) {
            // 获取去掉 "get" 后的部分
            String withoutGet = originalName.substring(3);
            // 检查第一个字符是否为大写字母（标准的 getter 格式）
            if (Character.isUpperCase(withoutGet.charAt(0))) {
                // 将首字母转换为小写
                return Character.toLowerCase(withoutGet.charAt(0)) + withoutGet.substring(1);
            }
        }
        // 不符合 getter 格式，返回原方法名
        return originalName;
    }

    /**
     * 转换方法名，自动去掉 get 前缀
     *
     * @param methods 方法数组
     * @return 转换后的方法名
     */
    public static String[] transformMethodNames(Method[] methods) {
        Set<String> originalNames = Arrays.stream(methods).map(Method::getName).collect(Collectors.toSet());
        return Arrays.stream(methods)
                .map(method -> {
                    String transformedName = StringUtils.transformMethodName(method.getName());
                    // 如果转换后的名字已经存在于原始名字中，则不转换
                    if (originalNames.contains(transformedName)) {
                        return method.getName();
                    }
                    return transformedName;
                }).toArray(String[]::new);
    }
}