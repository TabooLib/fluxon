package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.DestructuringAssignExpression;
import org.tabooproject.fluxon.parser.expression.IndexAccessExpression;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.parser.expression.TryExpression;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Lambda 捕获使用分析器。
 * 解析阶段先保留候选捕获，解析完成后再确认 body 是否实际访问父槽位。
 */
final class LambdaCaptureAnalyzer {

    private LambdaCaptureAnalyzer() {}

    static boolean hasActualCapture(ParseResult body, int captureOffset) {
        if (captureOffset <= 0) return false;
        return hasActualCapture(body, captureOffset, newSetFromIdentityMap());
    }

    private static boolean hasActualCapture(Object value, int captureOffset, Set<Object> visited) {
        if (value == null) return false;
        if (value instanceof ParseResult) {
            if (!visited.add(value)) return false;
            ParseResult node = (ParseResult) value;
            if (hasCapturedSlot(node, captureOffset)) return true;
            return scanFields(node, captureOffset, visited);
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                if (hasActualCapture(item, captureOffset, visited)) return true;
            }
            return false;
        }
        if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (hasActualCapture(entry.getKey(), captureOffset, visited)) return true;
                if (hasActualCapture(entry.getValue(), captureOffset, visited)) return true;
            }
            return false;
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (hasActualCapture(Array.get(value, i), captureOffset, visited)) return true;
            }
        }
        return false;
    }

    private static boolean hasCapturedSlot(ParseResult node, int captureOffset) {
        if (node instanceof ReferenceExpression) {
            return isCapturedPosition(((ReferenceExpression) node).getPosition(), captureOffset);
        }
        if (node instanceof AssignExpression) {
            return isCapturedPosition(((AssignExpression) node).getPosition(), captureOffset);
        }
        if (node instanceof IndexAccessExpression) {
            return isCapturedPosition(((IndexAccessExpression) node).getPosition(), captureOffset);
        }
        if (node instanceof TryExpression) {
            return isCapturedPosition(((TryExpression) node).getPosition(), captureOffset);
        }
        if (node instanceof DestructuringAssignExpression) {
            for (int position : ((DestructuringAssignExpression) node).getVariables().values()) {
                if (isCapturedPosition(position, captureOffset)) return true;
            }
        }
        return false;
    }

    private static boolean scanFields(ParseResult node, int captureOffset, Set<Object> visited) {
        Class<?> type = node.getClass();
        while (type != null && type != Object.class) {
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                try {
                    if (hasActualCapture(field.get(node), captureOffset, visited)) return true;
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException("Cannot scan lambda capture field: " + field.getName(), ex);
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean isCapturedPosition(int position, int captureOffset) {
        return position >= 0 && position < captureOffset;
    }

    private static Set<Object> newSetFromIdentityMap() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
