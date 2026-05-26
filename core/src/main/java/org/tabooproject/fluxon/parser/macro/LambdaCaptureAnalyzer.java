package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.DestructuringAssignExpression;
import org.tabooproject.fluxon.parser.expression.IndexAccessExpression;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.parser.expression.TryExpression;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
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

    private static void collectCapturedPositions(ParseResult node, int captureOffset, Set<Object> visited, Set<Integer> positions) {
        if (node == null || !visited.add(node)) return;
        collectCapturedSlot(node, captureOffset, positions);
        node.forEachChild(child -> collectCapturedPositions(child, captureOffset, visited, positions));
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

    private static void addCapturedPosition(int position, int captureOffset, Set<Integer> positions) {
        if (position >= 0 && position < captureOffset) {
            positions.add(position);
        }
    }

    private static Set<Object> newSetFromIdentityMap() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
