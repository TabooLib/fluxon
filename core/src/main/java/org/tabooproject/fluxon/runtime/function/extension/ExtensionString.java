package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

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

    @SuppressWarnings("DuplicatedCode")
    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(String.class)
                // 获取字符串长度
                .function("length", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    return str.length();
                })
                // 去除两端空白
                .function("trim", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    return str.trim();
                })
                // 去除左侧空白
                .function("ltrim", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    return str.replaceAll("^\\s+", "");
                })
                // 去除右侧空白
                .function("rtrim", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    return str.replaceAll("\\s+$", "");
                })
                // 字符串分割
                .function("split", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String delimiter = Coerce.asString(context.getArgument(0)).orElse("");
                    return Arrays.asList(str.split(delimiter));
                })
                // 字符串替换
                .function("replace", 2, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String oldStr = Coerce.asString(context.getArgument(0)).orElse("");
                    String newStr = Coerce.asString(context.getArgument(1)).orElse("");
                    return str.replace(oldStr, newStr);
                })
                // 字符串替换（全部）
                .function("replaceAll", 2, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String regex = Coerce.asString(context.getArgument(0)).orElse("");
                    String replacement = Coerce.asString(context.getArgument(1)).orElse("");
                    return str.replaceAll(regex, replacement);
                })
                // 获取子字符串
                .function("substring", Arrays.asList(1, 2), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int start = Coerce.asInteger(context.getArgument(0)).orElse(0);
                    if (context.hasArgument(1)) {
                        int end = Coerce.asInteger(context.getArgument(1)).orElse(str.length());
                        return str.substring(start, Math.min(end, str.length()));
                    }
                    return str.substring(start);
                })
                // 查找子字符串位置
                .function("indexOf", Arrays.asList(1, 2), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String searchStr = Coerce.asString(context.getArgument(0)).orElse("");
                    if (context.hasArgument(1)) {
                        int fromIndex = Coerce.asInteger(context.getArgument(1)).orElse(0);
                        return str.indexOf(searchStr, fromIndex);
                    }
                    return str.indexOf(searchStr);
                })
                // 查找子字符串最后位置
                .function("lastIndexOf", Arrays.asList(1, 2), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String searchStr = Coerce.asString(context.getArgument(0)).orElse("");
                    if (context.hasArgument(1)) {
                        int fromIndex = Coerce.asInteger(context.getArgument(1)).orElse(str.length());
                        return str.lastIndexOf(searchStr, fromIndex);
                    }
                    return str.lastIndexOf(searchStr);
                })
                // 转换为小写
                .function("lowercase", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    return str.toLowerCase();
                })
                // 转换为大写
                .function("uppercase", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    return str.toUpperCase();
                })
                // 检查是否以指定字符串开始
                .function("startsWith", Arrays.asList(1, 2), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String prefix = Coerce.asString(context.getArgument(0)).orElse("");
                    if (context.hasArgument(1)) {
                        int offset = Coerce.asInteger(context.getArgument(1)).orElse(0);
                        return str.startsWith(prefix, offset);
                    }
                    return str.startsWith(prefix);
                })
                // 检查是否以指定字符串结束
                .function("endsWith", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String suffix = Coerce.asString(context.getArgument(0)).orElse("");
                    return str.endsWith(suffix);
                })
                // 左填充
                .function("padLeft", Arrays.asList(1, 2), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int totalLength = Coerce.asInteger(context.getArgument(0)).orElse(0);
                    String padChar = context.hasArgument(1) ? Coerce.asString(context.getArgument(1)).orElse(" ") : " ";
                    if (padChar.isEmpty()) padChar = " ";

                    StringBuilder result = new StringBuilder(str);
                    while (result.length() < totalLength) {
                        result.insert(0, padChar.charAt(0));
                    }
                    return result.toString();
                })
                // 右填充
                .function("padRight", Arrays.asList(1, 2), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int totalLength = Coerce.asInteger(context.getArgument(0)).orElse(0);
                    String padChar = context.hasArgument(1) ? Coerce.asString(context.getArgument(1)).orElse(" ") : " ";
                    if (padChar.isEmpty()) padChar = " ";

                    StringBuilder result = new StringBuilder(str);
                    while (result.length() < totalLength) {
                        result.append(padChar.charAt(0));
                    }
                    return result.toString();
                })
                // 检查是否匹配正则表达式
                .function("matches", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String regex = Coerce.asString(context.getArgument(0)).orElse("");
                    return str.matches(regex);
                })
                // 检查是否包含子字符串
                .function("contains", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String searchStr = Coerce.asString(context.getArgument(0)).orElse("");
                    return str.contains(searchStr);
                })
                // 重复字符串
                .function("repeat", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int count = Coerce.asInteger(context.getArgument(0)).orElse(0);
                    if (count <= 0) return "";
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < count; i++) {
                        result.append(str);
                    }
                    return result.toString();
                })
                // 获取字符
                .function("charAt", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int index = Coerce.asInteger(context.getArgument(0)).orElse(0);
                    if (index < 0 || index >= str.length()) {
                        throw new IndexOutOfBoundsException("String index out of range: " + index);
                    }
                    return String.valueOf(str.charAt(index));
                })
                // 获取字符编码
                .function("charCodeAt", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int index = Coerce.asInteger(context.getArgument(0)).orElse(0);
                    if (index < 0 || index >= str.length()) {
                        throw new IndexOutOfBoundsException("String index out of range: " + index);
                    }
                    return (int) str.charAt(index);
                })
                // 转为字符数组
                .function("toCharArray", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    char[] chars = str.toCharArray();
                    List<String> result = new ArrayList<>();
                    for (char c : chars) {
                        result.add(String.valueOf(c));
                    }
                    return result;
                })
                // 判断是否为空或空白
                .function("isEmpty", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    return str.isEmpty();
                })
                .function("isBlank", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    return str.trim().isEmpty();
                })
                // 反转字符串
                .function("reverse", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    return new StringBuilder(str).reverse().toString();
                })
                // 首字母大写
                .function("capitalize", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    if (str.isEmpty()) return str;
                    return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
                })
                // 提取所有匹配的子字符串
                .function("findAll", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String regex = Coerce.asString(context.getArgument(0)).orElse("");
                    List<String> matches = new ArrayList<>();
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(str);
                    while (matcher.find()) {
                        matches.add(matcher.group());
                    }
                    return matches;
                });
    }
}