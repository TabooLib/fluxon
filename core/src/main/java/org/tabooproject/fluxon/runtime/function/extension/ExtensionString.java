package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

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
                .function("length", returns(Type.I).noParams(), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnInt(str.length());
                })
                // 去除两端空白
                .function("trim", returns(Type.STRING).noParams(), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(str.trim());
                })
                // 去除左侧空白
                .function("ltrim", returns(Type.STRING).noParams(), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(str.replaceAll("^\\s+", ""));
                })
                // 去除右侧空白
                .function("rtrim", returns(Type.STRING).noParams(), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(str.replaceAll("\\s+$", ""));
                })
                // 字符串分割
                .function("split", returns(Type.OBJECT).params(Type.STRING), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String delimiter = (String) context.getRef(0);
                    context.setReturnRef(Arrays.asList(str.split(delimiter != null ? delimiter : "")));
                })
                // 字符串替换
                .function("replace", returns(Type.STRING).params(Type.STRING, Type.STRING), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String oldStr = (String) context.getRef(0);
                    String newStr = (String) context.getRef(1);
                    context.setReturnRef(str.replace(oldStr != null ? oldStr : "", newStr != null ? newStr : ""));
                })
                // 字符串替换（全部）
                .function("replaceAll", returns(Type.STRING).params(Type.STRING, Type.STRING), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String regex = (String) context.getRef(0);
                    String replacement = (String) context.getRef(1);
                    context.setReturnRef(str.replaceAll(regex != null ? regex : "", replacement != null ? replacement : ""));
                })
                // 获取子字符串 (1-2 params)
                .function("substring", returns(Type.STRING).params(Type.I, Type.I), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int start = Coerce.asInteger(context.getArgBoxed(0)).orElse(0);
                    if (context.getArgumentCount() < 2) {
                        context.setReturnRef(str.substring(start));
                    } else {
                        int end = Coerce.asInteger(context.getArgBoxed(1)).orElse(str.length());
                        context.setReturnRef(str.substring(start, Math.min(end, str.length())));
                    }
                })
                // 查找子字符串位置 (1-2 params)
                .function("indexOf", returns(Type.I).params(Type.STRING, Type.I), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String searchStr = (String) context.getRef(0);
                    if (searchStr == null) searchStr = "";
                    if (context.getArgumentCount() < 2) {
                        context.setReturnInt(str.indexOf(searchStr));
                    } else {
                        int fromIndex = Coerce.asInteger(context.getArgBoxed(1)).orElse(0);
                        context.setReturnInt(str.indexOf(searchStr, fromIndex));
                    }
                })
                // 查找子字符串最后位置 (1-2 params)
                .function("lastIndexOf", returns(Type.I).params(Type.STRING, Type.I), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String searchStr = (String) context.getRef(0);
                    if (searchStr == null) searchStr = "";
                    if (context.getArgumentCount() < 2) {
                        context.setReturnInt(str.lastIndexOf(searchStr));
                    } else {
                        int fromIndex = Coerce.asInteger(context.getArgBoxed(1)).orElse(str.length());
                        context.setReturnInt(str.lastIndexOf(searchStr, fromIndex));
                    }
                })
                // 转换为小写
                .function("lowercase", returns(Type.STRING).noParams(), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(str.toLowerCase());
                })
                // 转换为大写
                .function("uppercase", returns(Type.STRING).noParams(), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(str.toUpperCase());
                })
                // 检查是否以指定字符串开始 (1-2 params)
                .function("startsWith", returns(Type.Z).params(Type.STRING, Type.I), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String prefix = (String) context.getRef(0);
                    if (prefix == null) prefix = "";
                    if (context.getArgumentCount() < 2) {
                        context.setReturnBool(str.startsWith(prefix));
                    } else {
                        int offset = Coerce.asInteger(context.getArgBoxed(1)).orElse(0);
                        context.setReturnBool(str.startsWith(prefix, offset));
                    }
                })
                // 检查是否以指定字符串结束
                .function("endsWith", returns(Type.Z).params(Type.STRING), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String suffix = (String) context.getRef(0);
                    context.setReturnBool(str.endsWith(suffix != null ? suffix : ""));
                })
                // 左填充 (1-2 params)
                .function("padLeft", returns(Type.STRING).params(Type.I, Type.STRING), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int totalLength = Coerce.asInteger(context.getArgBoxed(0)).orElse(0);
                    char padChar = ' ';
                    if (context.getArgumentCount() >= 2) {
                        String padStr = (String) context.getRef(1);
                        if (padStr != null && !padStr.isEmpty()) padChar = padStr.charAt(0);
                    }
                    StringBuilder result = new StringBuilder(str);
                    while (result.length() < totalLength) {
                        result.insert(0, padChar);
                    }
                    context.setReturnRef(result.toString());
                })
                // 右填充 (1-2 params)
                .function("padRight", returns(Type.STRING).params(Type.I, Type.STRING), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int totalLength = Coerce.asInteger(context.getArgBoxed(0)).orElse(0);
                    char padChar = ' ';
                    if (context.getArgumentCount() >= 2) {
                        String padStr = (String) context.getRef(1);
                        if (padStr != null && !padStr.isEmpty()) padChar = padStr.charAt(0);
                    }
                    StringBuilder result = new StringBuilder(str);
                    while (result.length() < totalLength) {
                        result.append(padChar);
                    }
                    context.setReturnRef(result.toString());
                })
                // 检查是否匹配正则表达式
                .function("matches", returns(Type.Z).params(Type.STRING), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String regex = (String) context.getRef(0);
                    context.setReturnBool(str.matches(regex != null ? regex : ""));
                })
                // 检查是否包含子字符串
                .function("contains", returns(Type.Z).params(Type.STRING), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String searchStr = (String) context.getRef(0);
                    context.setReturnBool(str.contains(searchStr != null ? searchStr : ""));
                })
                // 重复字符串
                .function("repeat", returns(Type.STRING).params(Type.I), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int count = Coerce.asInteger(context.getArgBoxed(0)).orElse(0);
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
                .function("charAt", returns(Type.STRING).params(Type.I), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int index = Coerce.asInteger(context.getArgBoxed(0)).orElse(0);
                    if (index < 0 || index >= str.length()) {
                        throw new IndexOutOfBoundsException("String index out of range: " + index);
                    }
                    context.setReturnRef(String.valueOf(str.charAt(index)));
                })
                // 获取字符编码
                .function("charCodeAt", returns(Type.I).params(Type.I), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    int index = Coerce.asInteger(context.getArgBoxed(0)).orElse(0);
                    if (index < 0 || index >= str.length()) {
                        throw new IndexOutOfBoundsException("String index out of range: " + index);
                    }
                    context.setReturnInt(str.charAt(index));
                })
                // 转为字符数组
                .function("toCharArray", returns(Type.OBJECT).noParams(), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    char[] chars = str.toCharArray();
                    List<String> result = new ArrayList<>();
                    for (char c : chars) {
                        result.add(String.valueOf(c));
                    }
                    context.setReturnRef(result);
                })
                // 判断是否为空或空白
                .function("isEmpty", returns(Type.Z).noParams(), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(str.isEmpty());
                })
                .function("isBlank", returns(Type.Z).noParams(), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(str.trim().isEmpty());
                })
                // 反转字符串
                .function("reverse", returns(Type.STRING).noParams(), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(new StringBuilder(str).reverse().toString());
                })
                // 首字母大写
                .function("capitalize", returns(Type.STRING).noParams(), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    if (str.isEmpty()) {
                        context.setReturnRef(str);
                        return;
                    }
                    context.setReturnRef(Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase());
                })
                // 提取所有匹配的子字符串
                .function("findAll", returns(Type.OBJECT).params(Type.STRING), (context) -> {
                    String str = Objects.requireNonNull(context.getTarget());
                    String regex = (String) context.getRef(0);
                    List<String> matches = new ArrayList<>();
                    if (regex != null && !regex.isEmpty()) {
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(str);
                        while (matcher.find()) {
                            matches.add(matcher.group());
                        }
                    }
                    context.setReturnRef(matches);
                });
    }
}
