package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.IndexAccessExpression;
import org.tabooproject.fluxon.runtime.Type;

import java.util.List;
import java.util.Map;

/**
 * 索引访问表达式求值器
 *
 * @author sky
 */
public class IndexAccessEvaluator extends ExpressionEvaluator<IndexAccessExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.INDEX_ACCESS;
    }

    @Override
    public Object evaluate(Interpreter interpreter, IndexAccessExpression expr) {
        Object target = interpreter.evaluate(expr.getTarget());
        List<ParseResult> indices = expr.getIndices();

        // 单索引访问
        if (indices.size() == 1) {
            Object index = interpreter.evaluate(indices.get(0));
            return getIndex(target, index);
        }
        // 多索引访问：嵌套获取 map["k1", "k2"] = map["k1"]["k2"]
        else {
            Object current = target;
            for (ParseResult indexExpr : indices) {
                Object index = interpreter.evaluate(indexExpr);
                current = getIndex(current, index);
            }
            return current;
        }
    }

    /**
     * 执行单次索引访问
     */
    private Object getIndex(Object target, Object index) {
        if (target instanceof List) {
            int idx = ((Number) index).intValue();
            List<?> list = (List<?>) target;
            if (idx < 0 || idx >= list.size()) {
                throw new IndexOutOfBoundsException("Index: " + idx + ", Size: " + list.size());
            }
            return list.get(idx);
        } else if (target instanceof Map) {
            return ((Map<?, ?>) target).get(index);
        } else if (target instanceof String) {
            int idx = ((Number) index).intValue();
            String str = (String) target;
            if (idx < 0 || idx >= str.length()) {
                throw new IndexOutOfBoundsException("Index: " + idx + ", Length: " + str.length());
            }
            return String.valueOf(str.charAt(idx));
        } else if (target instanceof Object[]) {
            int idx = ((Number) index).intValue();
            Object[] arr = (Object[]) target;
            if (idx < 0 || idx >= arr.length) {
                throw new IndexOutOfBoundsException("Index: " + idx + ", Length: " + arr.length);
            }
            return arr[idx];
        } else {
            throw new RuntimeException("Cannot index type: " + (target == null ? "null" : target.getClass().getName()));
        }
    }

    @Override
    public Type generateBytecode(IndexAccessExpression expr, CodeContext ctx, MethodVisitor mv) {
        // TODO: 字节码生成
        return Type.OBJECT;
    }
}
