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
        // 获取字符串长度
        runtime.registerExtensionFunction(String.class, "length", 0, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            return str.length();
        });

        // 去除两端空白
        runtime.registerExtensionFunction(String.class, "trim", 0, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            return str.trim();
        });

        // 去除左侧空白
        runtime.registerExtensionFunction(String.class, "ltrim", 0, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            return str.replaceAll("^\\s+", "");
        });

        // 去除右侧空白
        runtime.registerExtensionFunction(String.class, "rtrim", 0, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            return str.replaceAll("\\s+$", "");
        });

        // 字符串分割
        runtime.registerExtensionFunction(String.class, "split", 1, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            Object[] args = context.getArguments();
            String delimiter = args[0].toString();
            return Arrays.asList(str.split(delimiter));
        });

        // 字符串替换
        runtime.registerExtensionFunction(String.class, "replace", 2, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            Object[] args = context.getArguments();
            String oldStr = Coerce.asString(args[0]).orElse("");
            String newStr = Coerce.asString(args[1]).orElse("");
            return str.replace(oldStr, newStr);
        });

        // 字符串替换（全部）
        runtime.registerExtensionFunction(String.class, "replaceAll", 2, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            Object[] args = context.getArguments();
            String regex = Coerce.asString(args[0]).orElse("");
            String replacement = Coerce.asString(args[1]).orElse("");
            return str.replaceAll(regex, replacement);
        });

        // 获取子字符串
        runtime.registerExtensionFunction(String.class, "substring", Arrays.asList(1, 2), (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            Object[] args = context.getArguments();
            int start = Coerce.asInteger(args[0]).orElse(0);
            if (args.length > 1) {
                int end = Coerce.asInteger(args[1]).orElse(str.length());
                return str.substring(start, Math.min(end, str.length()));
            }
            return str.substring(start);
        });

        // 查找子字符串位置
        runtime.registerExtensionFunction(String.class, "indexOf", Arrays.asList(1, 2), (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            Object[] args = context.getArguments();
            String searchStr = Coerce.asString(args[0]).orElse("");
            if (args.length > 1) {
                int fromIndex = Coerce.asInteger(args[1]).orElse(0);
                return str.indexOf(searchStr, fromIndex);
            }
            return str.indexOf(searchStr);
        });

        // 查找子字符串最后位置
        runtime.registerExtensionFunction(String.class, "lastIndexOf", Arrays.asList(1, 2), (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            Object[] args = context.getArguments();
            String searchStr = Coerce.asString(args[0]).orElse("");
            if (args.length > 1) {
                int fromIndex = Coerce.asInteger(args[1]).orElse(str.length());
                return str.lastIndexOf(searchStr, fromIndex);
            }
            return str.lastIndexOf(searchStr);
        });

        // 转换为小写
        runtime.registerExtensionFunction(String.class, "lowercase", 0, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            return str.toLowerCase();
        });

        // 转换为大写
        runtime.registerExtensionFunction(String.class, "uppercase", 0, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            return str.toUpperCase();
        });

        // 检查是否以指定字符串开始
        runtime.registerExtensionFunction(String.class, "startsWith", Arrays.asList(1, 2), (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            Object[] args = context.getArguments();
            String prefix = Coerce.asString(args[0]).orElse("");
            if (args.length > 1) {
                int offset = Coerce.asInteger(args[1]).orElse(0);
                return str.startsWith(prefix, offset);
            }
            return str.startsWith(prefix);
        });

        // 检查是否以指定字符串结束
        runtime.registerExtensionFunction(String.class, "endsWith", 1, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            String suffix = Coerce.asString(context.getArguments()[0]).orElse("");
            return str.endsWith(suffix);
        });

        // 左填充
        runtime.registerExtensionFunction(String.class, "padLeft", Arrays.asList(1, 2), (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            Object[] args = context.getArguments();
            int totalLength = Coerce.asInteger(args[0]).orElse(0);
            String padChar = args.length > 1 ? Coerce.asString(args[1]).orElse(" ") : " ";
            if (padChar.isEmpty()) padChar = " ";

            StringBuilder result = new StringBuilder(str);
            while (result.length() < totalLength) {
                result.insert(0, padChar.charAt(0));
            }
            return result.toString();
        });

        // 右填充
        runtime.registerExtensionFunction(String.class, "padRight", Arrays.asList(1, 2), (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            Object[] args = context.getArguments();
            int totalLength = Coerce.asInteger(args[0]).orElse(0);
            String padChar = args.length > 1 ? Coerce.asString(args[1]).orElse(" ") : " ";
            if (padChar.isEmpty()) padChar = " ";

            StringBuilder result = new StringBuilder(str);
            while (result.length() < totalLength) {
                result.append(padChar.charAt(0));
            }
            return result.toString();
        });

        // 检查是否匹配正则表达式
        runtime.registerExtensionFunction(String.class, "matches", 1, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            String regex = Coerce.asString(context.getArguments()[0]).orElse("");
            return str.matches(regex);
        });

        // 检查是否包含子字符串
        runtime.registerExtensionFunction(String.class, "contains", 1, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            String searchStr = Coerce.asString(context.getArguments()[0]).orElse("");
            return str.contains(searchStr);
        });

        // 重复字符串
        runtime.registerExtensionFunction(String.class, "repeat", 1, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            int count = Coerce.asInteger(context.getArguments()[0]).orElse(0);
            if (count <= 0) return "";
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < count; i++) {
                result.append(str);
            }
            return result.toString();
        });

        // 获取字符
        runtime.registerExtensionFunction(String.class, "charAt", 1, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            int index = Coerce.asInteger(context.getArguments()[0]).orElse(0);
            if (index < 0 || index >= str.length()) {
                throw new IndexOutOfBoundsException("String index out of range: " + index);
            }
            return String.valueOf(str.charAt(index));
        });

        // 获取字符编码
        runtime.registerExtensionFunction(String.class, "charCodeAt", 1, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            int index = Coerce.asInteger(context.getArguments()[0]).orElse(0);
            if (index < 0 || index >= str.length()) {
                throw new IndexOutOfBoundsException("String index out of range: " + index);
            }
            return (int) str.charAt(index);
        });

        // 转为字符数组
        runtime.registerExtensionFunction(String.class, "toCharArray", 0, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            char[] chars = str.toCharArray();
            List<String> result = new ArrayList<>();
            for (char c : chars) {
                result.add(String.valueOf(c));
            }
            return result;
        });

        // 判断是否为空或空白
        runtime.registerExtensionFunction(String.class, "isEmpty", 0, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            return str.isEmpty();
        });

        runtime.registerExtensionFunction(String.class, "isBlank", 0, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            return str.trim().isEmpty();
        });

        // 反转字符串
        runtime.registerExtensionFunction(String.class, "reverse", 0, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            return new StringBuilder(str).reverse().toString();
        });

        // 首字母大写
        runtime.registerExtensionFunction(String.class, "capitalize", 0, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            if (str.isEmpty()) return str;
            return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
        });

        // 提取所有匹配的子字符串
        runtime.registerExtensionFunction(String.class, "findAll", 1, (context) -> {
            String str = (String) Objects.requireNonNull(context.getTarget());
            String regex = Coerce.asString(context.getArguments()[0]).orElse("");
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