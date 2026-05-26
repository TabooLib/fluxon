package org.tabooproject.fluxon.parser;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AST 子节点遍历工具。
 * 只把 ParseResult 当作节点展开，其他运行时对象只作为字段值跳过。
 */
final class ParseResultChildren {

    private ParseResultChildren() {}

    static void forEach(ParseResult node, Consumer<ParseResult> consumer) {
        Class<?> type = node.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                try {
                    visitValue(field.get(node), consumer);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException("Cannot scan AST child field: " + field.getName(), ex);
                }
            }
            type = type.getSuperclass();
        }
    }

    private static void visitValue(Object value, Consumer<ParseResult> consumer) {
        if (value == null) return;
        if (value instanceof ParseResult) {
            consumer.accept((ParseResult) value);
            return;
        }
        if (value instanceof ParseResultContainer) {
            ((ParseResultContainer) value).forEachChild(consumer);
            return;
        }
        if (value instanceof Collection<?>) {
            for (Object item : (Collection<?>) value) {
                visitValue(item, consumer);
            }
            return;
        }
        if (value instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                visitValue(entry.getKey(), consumer);
                visitValue(entry.getValue(), consumer);
            }
            return;
        }
        Class<?> type = value.getClass();
        if (!type.isArray()) return;
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            visitValue(Array.get(value, i), consumer);
        }
    }
}
