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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lambda 捕获使用分析器。
 * 解析阶段先保留候选捕获，解析完成后再确认 body 是否实际访问父槽位。
 */
final class LambdaCaptureAnalyzer {

    private LambdaCaptureAnalyzer() {}

    static boolean hasActualCapture(ParseResult body, int captureOffset) {
        return !findCapturedPositions(body, captureOffset).isEmpty();
    }

    static Set<Integer> findCapturedPositions(ParseResult body, int captureOffset) {
        if (captureOffset <= 0) return Collections.emptySet();
        Set<Integer> positions = new LinkedHashSet<>();
        collectCapturedPositions(body, captureOffset, newSetFromIdentityMap(), positions);
        return positions;
    }

    private static void collectCapturedPositions(Object value, int captureOffset, Set<Object> visited, Set<Integer> positions) {
        if (value == null) return;
        if (value instanceof ParseResult) {
            if (!visited.add(value)) return;
            ParseResult node = (ParseResult) value;
            collectCapturedSlot(node, captureOffset, positions);
            scanFields(node, captureOffset, visited, positions);
            return;
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                collectCapturedPositions(item, captureOffset, visited, positions);
            }
            return;
        }
        if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                collectCapturedPositions(entry.getKey(), captureOffset, visited, positions);
                collectCapturedPositions(entry.getValue(), captureOffset, visited, positions);
            }
            return;
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                collectCapturedPositions(Array.get(value, i), captureOffset, visited, positions);
            }
        }
    }

    private static void collectCapturedSlot(ParseResult node, int captureOffset, Set<Integer> positions) {
        if (node instanceof ReferenceExpression) {
            addCapturedPosition(((ReferenceExpression) node).getPosition(), captureOffset, positions);
            return;
        }
        if (node instanceof AssignExpression) {
            addCapturedPosition(((AssignExpression) node).getPosition(), captureOffset, positions);
            return;
        }
        if (node instanceof IndexAccessExpression) {
            addCapturedPosition(((IndexAccessExpression) node).getPosition(), captureOffset, positions);
            return;
        }
        if (node instanceof TryExpression) {
            addCapturedPosition(((TryExpression) node).getPosition(), captureOffset, positions);
            return;
        }
        if (node instanceof DestructuringAssignExpression) {
            for (int position : ((DestructuringAssignExpression) node).getVariables().values()) {
                addCapturedPosition(position, captureOffset, positions);
            }
        }
    }

    private static void scanFields(ParseResult node, int captureOffset, Set<Object> visited, Set<Integer> positions) {
        Class<?> type = node.getClass();
        while (type != null && type != Object.class) {
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                try {
                    collectCapturedPositions(field.get(node), captureOffset, visited, positions);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException("Cannot scan lambda capture field: " + field.getName(), ex);
                }
            }
            type = type.getSuperclass();
        }
    }

    private static void addCapturedPosition(int position, int captureOffset, Set<Integer> positions) {
        if (position >= 0 && position < captureOffset) {
            positions.add(position);
        }
    }

    private static Set<Object> newSetFromIdentityMap() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
