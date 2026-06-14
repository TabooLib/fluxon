package org.tabooproject.fluxon.compiler.analysis;

import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.LambdaExpression;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.expression.literal.Literal;
import org.tabooproject.fluxon.parser.expression.literal.NullLiteral;
import org.tabooproject.fluxon.parser.expression.literal.StringLiteral;

import java.util.Map;

/**
 * 从 AST 节点解析编译期可确定的标量值。
 * 支持字面量、裸标识符（字符串）、lambda 字面量、以及已记录局部常量上的 {@code &name} 引用。
 */
public final class StaticValueUtil {

    private StaticValueUtil() {
    }

    /**
     * 解析节点在编译期的静态值；无法确定时返回 {@code null}。
     *
     * @param node AST 节点
     * @param locals 当前函数体内已传播的局部常量
     * @return 静态值，或 {@code null} 表示该节点未解析
     */
    public static Object resolve(ParseResult node, Map<String, Object> locals) {
        if (node == null) {
            return null;
        }
        if (node instanceof StringLiteral) {
            return ((StringLiteral) node).getValue();
        }
        if (node instanceof Identifier) {
            return ((Identifier) node).getValue();
        }
        if (node instanceof Literal) {
            if (node instanceof NullLiteral) {
                return null;
            }
            return ((Literal) node).getSourceValue();
        }
        if (node instanceof LambdaExpression) {
            return node;
        }
        if (node instanceof ReferenceExpression) {
            String name = ((ReferenceExpression) node).getIdentifier().getValue();
            if (locals == null || !locals.containsKey(name)) {
                return null;
            }
            return locals.get(name);
        }
        return null;
    }

    /**
     * 该实参在编译期是否可确定静态值（含 {@code null} 字面量、已绑定引用）。
     */
    public static boolean isStaticallyKnown(ParseResult node, Map<String, Object> locals) {
        if (node == null) {
            return false;
        }
        if (node instanceof ReferenceExpression) {
            String name = ((ReferenceExpression) node).getIdentifier().getValue();
            return locals != null && locals.containsKey(name);
        }
        return node instanceof Literal || node instanceof Identifier || node instanceof LambdaExpression;
    }
}