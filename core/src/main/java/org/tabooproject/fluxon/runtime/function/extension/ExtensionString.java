package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串扩展函数
 *
 * @author sky
 */
public class ExtensionString {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionString.class);
    }

    // 获取字符串长度
    @FluxonFunction(value = "length", target = String.class)
    public static int length(String str) {
        return Objects.requireNonNull(str).length();
    }

    // 去除两端空白
    @FluxonFunction(value = "trim", target = String.class)
    public static String trim(String str) {
        return Objects.requireNonNull(str).trim();
    }

    // 去除左侧空白
    @FluxonFunction(value = "ltrim", target = String.class)
    public static String ltrim(String str) {
        return Objects.requireNonNull(str).replaceAll("^\\s+", "");
    }

    // 去除右侧空白
    @FluxonFunction(value = "rtrim", target = String.class)
    public static String rtrim(String str) {
        return Objects.requireNonNull(str).replaceAll("\\s+$", "");
    }

    // 字符串分割
    @FluxonFunction(value = "split", target = String.class)
    public static Object split(String str, String delimiter) {
        Objects.requireNonNull(str);
        return Arrays.asList(str.split(delimiter != null ? delimiter : ""));
    }

    // 字符串替换
    @FluxonFunction(value = "replace", target = String.class)
    public static String replace(String str, String oldStr, String newStr) {
        Objects.requireNonNull(str);
        return str.replace(oldStr != null ? oldStr : "", newStr != null ? newStr : "");
    }

    // 字符串替换（全部）
    @FluxonFunction(value = "replaceAll", target = String.class)
    public static String replaceAll(String str, String regex, String replacement) {
        Objects.requireNonNull(str);
        return str.replaceAll(regex != null ? regex : "", replacement != null ? replacement : "");
    }

    // 获取子字符串
    @FluxonFunction(value = "substring", target = String.class)
    public static String substring1(String str, int start) {
        return Objects.requireNonNull(str).substring(start);
    }

    @FluxonFunction(value = "substring", target = String.class)
    public static String substring2(String str, int start, int end) {
        Objects.requireNonNull(str);
        return str.substring(start, Math.min(end, str.length()));
    }

    // 查找子字符串位置
    @FluxonFunction(value = "indexOf", target = String.class)
    public static int indexOf1(String str, String searchStr) {
        Objects.requireNonNull(str);
        if (searchStr == null) searchStr = "";
        return str.indexOf(searchStr);
    }

    @FluxonFunction(value = "indexOf", target = String.class)
    public static int indexOf2(String str, String searchStr, int fromIndex) {
        Objects.requireNonNull(str);
        if (searchStr == null) searchStr = "";
        return str.indexOf(searchStr, fromIndex);
    }

    // 查找子字符串最后位置
    @FluxonFunction(value = "lastIndexOf", target = String.class)
    public static int lastIndexOf1(String str, String searchStr) {
        Objects.requireNonNull(str);
        if (searchStr == null) searchStr = "";
        return str.lastIndexOf(searchStr);
    }

    @FluxonFunction(value = "lastIndexOf", target = String.class)
    public static int lastIndexOf2(String str, String searchStr, int fromIndex) {
        Objects.requireNonNull(str);
        if (searchStr == null) searchStr = "";
        return str.lastIndexOf(searchStr, fromIndex);
    }

    // 转换为小写
    @FluxonFunction(value = "lowercase", target = String.class)
    public static String lowercase(String str) {
        return Objects.requireNonNull(str).toLowerCase();
    }

    // 转换为大写
    @FluxonFunction(value = "uppercase", target = String.class)
    public static String uppercase(String str) {
        return Objects.requireNonNull(str).toUpperCase();
    }

    // 检查是否以指定字符串开始
    @FluxonFunction(value = "startsWith", target = String.class)
    public static boolean startsWith1(String str, String prefix) {
        Objects.requireNonNull(str);
        if (prefix == null) prefix = "";
        return str.startsWith(prefix);
    }

    @FluxonFunction(value = "startsWith", target = String.class)
    public static boolean startsWith2(String str, String prefix, int offset) {
        Objects.requireNonNull(str);
        if (prefix == null) prefix = "";
        return str.startsWith(prefix, offset);
    }

    // 检查是否以指定字符串结束
    @FluxonFunction(value = "endsWith", target = String.class)
    public static boolean endsWith(String str, String suffix) {
        Objects.requireNonNull(str);
        return str.endsWith(suffix != null ? suffix : "");
    }

    // 左填充
    @FluxonFunction(value = "padLeft", target = String.class)
    public static String padLeft1(String str, int totalLength) {
        Objects.requireNonNull(str);
        StringBuilder result = new StringBuilder(str);
        while (result.length() < totalLength) {
            result.insert(0, ' ');
        }
        return result.toString();
    }

    @FluxonFunction(value = "padLeft", target = String.class)
    public static String padLeft2(String str, int totalLength, String padStr) {
        Objects.requireNonNull(str);
        char padChar = (padStr != null && !padStr.isEmpty()) ? padStr.charAt(0) : ' ';
        StringBuilder result = new StringBuilder(str);
        while (result.length() < totalLength) {
            result.insert(0, padChar);
        }
        return result.toString();
    }

    // 右填充
    @FluxonFunction(value = "padRight", target = String.class)
    public static String padRight1(String str, int totalLength) {
        Objects.requireNonNull(str);
        StringBuilder result = new StringBuilder(str);
        while (result.length() < totalLength) {
            result.append(' ');
        }
        return result.toString();
    }

    @FluxonFunction(value = "padRight", target = String.class)
    public static String padRight2(String str, int totalLength, String padStr) {
        Objects.requireNonNull(str);
        char padChar = (padStr != null && !padStr.isEmpty()) ? padStr.charAt(0) : ' ';
        StringBuilder result = new StringBuilder(str);
        while (result.length() < totalLength) {
            result.append(padChar);
        }
        return result.toString();
    }

    // 检查是否匹配正则表达式
    @FluxonFunction(value = "matches", target = String.class)
    public static boolean matches(String str, String regex) {
        Objects.requireNonNull(str);
        return str.matches(regex != null ? regex : "");
    }

    // 检查是否包含子字符串
    @FluxonFunction(value = "contains", target = String.class)
    public static boolean contains(String str, String searchStr) {
        Objects.requireNonNull(str);
        return str.contains(searchStr != null ? searchStr : "");
    }

    // 重复字符串
    @FluxonFunction(value = "repeat", target = String.class)
    public static String repeat(String str, int count) {
        Objects.requireNonNull(str);
        if (count <= 0) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(str);
        }
        return result.toString();
    }

    // 获取字符
    @FluxonFunction(value = "charAt", target = String.class)
    public static String charAt(String str, int index) {
        Objects.requireNonNull(str);
        if (index < 0 || index >= str.length()) {
            throw new IndexOutOfBoundsException("String index out of range: " + index);
        }
        return String.valueOf(str.charAt(index));
    }

    // 获取字符编码
    @FluxonFunction(value = "charCodeAt", target = String.class)
    public static int charCodeAt(String str, int index) {
        Objects.requireNonNull(str);
        if (index < 0 || index >= str.length()) {
            throw new IndexOutOfBoundsException("String index out of range: " + index);
        }
        return str.charAt(index);
    }

    // 转为字符数组
    @FluxonFunction(value = "toCharArray", target = String.class)
    public static Object toCharArray(String str) {
        Objects.requireNonNull(str);
        char[] chars = str.toCharArray();
        List<String> result = new ArrayList<>();
        for (char c : chars) {
            result.add(String.valueOf(c));
        }
        return result;
    }

    // 判断是否为空
    @FluxonFunction(value = "isEmpty", target = String.class)
    public static boolean isEmpty(String str) {
        return Objects.requireNonNull(str).isEmpty();
    }

    // 判断是否为空白
    @FluxonFunction(value = "isBlank", target = String.class)
    public static boolean isBlank(String str) {
        return Objects.requireNonNull(str).trim().isEmpty();
    }

    // 反转字符串
    @FluxonFunction(value = "reverse", target = String.class)
    public static String reverse(String str) {
        return new StringBuilder(Objects.requireNonNull(str)).reverse().toString();
    }

    // 首字母大写
    @FluxonFunction(value = "capitalize", target = String.class)
    public static String capitalize(String str) {
        Objects.requireNonNull(str);
        if (str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }

    // 提取所有匹配的子字符串
    @FluxonFunction(value = "findAll", target = String.class)
    public static Object findAll(String str, String regex) {
        Objects.requireNonNull(str);
        List<String> matches = new ArrayList<>();
        if (regex != null && !regex.isEmpty()) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(str);
            while (matcher.find()) {
                matches.add(matcher.group());
            }
        }
        return matches;
    }
}
