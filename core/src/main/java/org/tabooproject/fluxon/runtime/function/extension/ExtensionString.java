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
                    context.setReturnRef(str.length());
                })
                // 去除两端空白
                .function("trim", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(str.trim());
                })
                // 去除左侧空白
                .function("ltrim", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(str.replaceAll("^\\s+", ""));
                })
                // 去除右侧空白
                .function("rtrim", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(str.replaceAll("\\s+$", ""));
                })
                // 字符串分割
                .function("split", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String delimiter = Coerce.asString(context.getRef(0)).orElse("");
                    context.setReturnRef(Arrays.asList(str.split(delimiter)));
                })
                // 字符串替换
                .function("replace", 2, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String oldStr = Coerce.asString(context.getRef(0)).orElse("");
                    String newStr = Coerce.asString(context.getRef(1)).orElse("");
                    context.setReturnRef(str.replace(oldStr, newStr));
                })
                // 字符串替换（全部）
                .function("replaceAll", 2, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String regex = Coerce.asString(context.getRef(0)).orElse("");
                    String replacement = Coerce.asString(context.getRef(1)).orElse("");
                    context.setReturnRef(str.replaceAll(regex, replacement));
                })
                // 获取子字符串
                .function("substring", Arrays.asList(1, 2), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int start = Coerce.asInteger(context.getRef(0)).orElse(0);
                    if (1 < context.getArgumentCount()) {
                        int end = Coerce.asInteger(context.getRef(1)).orElse(str.length());
                        context.setReturnRef(str.substring(start, Math.min(end, str.length())));
                        return;
                    }
                    context.setReturnRef(str.substring(start));
                })
                // 查找子字符串位置
                .function("indexOf", Arrays.asList(1, 2), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String searchStr = Coerce.asString(context.getRef(0)).orElse("");
                    if (1 < context.getArgumentCount()) {
                        int fromIndex = Coerce.asInteger(context.getRef(1)).orElse(0);
                        context.setReturnRef(str.indexOf(searchStr, fromIndex));
                        return;
                    }
                    context.setReturnRef(str.indexOf(searchStr));
                })
                // 查找子字符串最后位置
                .function("lastIndexOf", Arrays.asList(1, 2), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String searchStr = Coerce.asString(context.getRef(0)).orElse("");
                    if (1 < context.getArgumentCount()) {
                        int fromIndex = Coerce.asInteger(context.getRef(1)).orElse(str.length());
                        context.setReturnRef(str.lastIndexOf(searchStr, fromIndex));
                        return;
                    }
                    context.setReturnRef(str.lastIndexOf(searchStr));
                })
                // 转换为小写
                .function("lowercase", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(str.toLowerCase());
                })
                // 转换为大写
                .function("uppercase", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(str.toUpperCase());
                })
                // 检查是否以指定字符串开始
                .function("startsWith", Arrays.asList(1, 2), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String prefix = Coerce.asString(context.getRef(0)).orElse("");
                    if (1 < context.getArgumentCount()) {
                        int offset = Coerce.asInteger(context.getRef(1)).orElse(0);
                        context.setReturnRef(str.startsWith(prefix, offset));
                        return;
                    }
                    context.setReturnRef(str.startsWith(prefix));
                })
                // 检查是否以指定字符串结束
                .function("endsWith", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String suffix = Coerce.asString(context.getRef(0)).orElse("");
                    context.setReturnRef(str.endsWith(suffix));
                })
                // 左填充
                .function("padLeft", Arrays.asList(1, 2), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int totalLength = Coerce.asInteger(context.getRef(0)).orElse(0);
                    String padChar = 1 < context.getArgumentCount() ? Coerce.asString(context.getRef(1)).orElse(" ") : " ";
                    if (padChar.isEmpty()) padChar = " ";
                    StringBuilder result = new StringBuilder(str);
                    while (result.length() < totalLength) {
                        result.insert(0, padChar.charAt(0));
                    }
                    context.setReturnRef(result.toString());
                })
                // 右填充
                .function("padRight", Arrays.asList(1, 2), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int totalLength = Coerce.asInteger(context.getRef(0)).orElse(0);
                    String padChar = 1 < context.getArgumentCount() ? Coerce.asString(context.getRef(1)).orElse(" ") : " ";
                    if (padChar.isEmpty()) padChar = " ";
                    StringBuilder result = new StringBuilder(str);
                    while (result.length() < totalLength) {
                        result.append(padChar.charAt(0));
                    }
                    context.setReturnRef(result.toString());
                })
                // 检查是否匹配正则表达式
                .function("matches", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String regex = Coerce.asString(context.getRef(0)).orElse("");
                    context.setReturnRef(str.matches(regex));
                })
                // 检查是否包含子字符串
                .function("contains", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String searchStr = Coerce.asString(context.getRef(0)).orElse("");
                    context.setReturnRef(str.contains(searchStr));
                })
                // 重复字符串
                .function("repeat", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int count = Coerce.asInteger(context.getRef(0)).orElse(0);
                    if (count <= 0) {
                        context.setReturnRef("");
                        return;
                    }
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < count; i++) {
                        result.append(str);
                    }
                    context.setReturnRef(result.toString());
                })
                // 获取字符
                .function("charAt", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int index = Coerce.asInteger(context.getRef(0)).orElse(0);
                    if (index < 0 || index >= str.length()) {
                        throw new IndexOutOfBoundsException("String index out of range: " + index);
                    }
                    context.setReturnRef(String.valueOf(str.charAt(index)));
                })
                // 获取字符编码
                .function("charCodeAt", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int index = Coerce.asInteger(context.getRef(0)).orElse(0);
                    if (index < 0 || index >= str.length()) {
                        throw new IndexOutOfBoundsException("String index out of range: " + index);
                    }
                    context.setReturnRef((int) str.charAt(index));
                })
                // 转为字符数组
                .function("toCharArray", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    char[] chars = str.toCharArray();
                    List<String> result = new ArrayList<>();
                    for (char c : chars) {
                        result.add(String.valueOf(c));
                    }
                    context.setReturnRef(result);
                })
                // 判断是否为空或空白
                .function("isEmpty", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(str.isEmpty());
                })
                .function("isBlank", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(str.trim().isEmpty());
                })
                // 反转字符串
                .function("reverse", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(new StringBuilder(str).reverse().toString());
                })
                // 首字母大写
                .function("capitalize", 0, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    if (str.isEmpty()) {
                        context.setReturnRef(str);
                        return;
                    }
                    context.setReturnRef(Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase());
                })
                // 提取所有匹配的子字符串
                .function("findAll", 1, (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String regex = Coerce.asString(context.getRef(0)).orElse("");
                    List<String> matches = new ArrayList<>();
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(str);
                    while (matcher.find()) {
                        matches.add(matcher.group());
                    }
                    context.setReturnRef(matches);
                });
    }
}
