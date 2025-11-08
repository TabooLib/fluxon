package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.IndexAccessExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

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
            return Intrinsics.getIndex(target, index);
        }
        // 多索引访问：嵌套获取 map["k1", "k2"] = map["k1"]["k2"]
        else {
            Object current = target;
            for (ParseResult indexExpr : indices) {
                Object index = interpreter.evaluate(indexExpr);
                current = Intrinsics.getIndex(current, index);
            }
            return current;
        }
    }

    @Override
    public Type generateBytecode(IndexAccessExpression expr, CodeContext ctx, MethodVisitor mv) {
        // 获取 target 的求值器
        Evaluator<ParseResult> targetEval = ctx.getEvaluator(expr.getTarget());
        if (targetEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for index access target");
        }
        // 生成 target 的字节码
        Type targetType = targetEval.generateBytecode(expr.getTarget(), ctx, mv);
        if (targetType == Type.VOID) {
            throw new VoidError("Void type is not allowed for index access target");
        }
        List<ParseResult> indices = expr.getIndices();
        // 对每个索引依次调用 Intrinsics.getIndex
        for (ParseResult indexExpr : indices) {
            // 获取索引的求值器
            Evaluator<ParseResult> indexEval = ctx.getEvaluator(indexExpr);
            if (indexEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for index expression");
            }
            // 生成索引的字节码
            Type indexType = indexEval.generateBytecode(indexExpr, ctx, mv);
            if (indexType == Type.VOID) {
                throw new VoidError("Void type is not allowed for index");
            }
            // 调用 Intrinsics.getIndex(Object target, Object index)
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    Intrinsics.TYPE.getPath(),
                    "getIndex",
                    "(" + Type.OBJECT + Type.OBJECT + ")" + Type.OBJECT,
                    false
            );
            // 如果还有更多索引，当前结果将作为下一次调用的 target
        }
        return Type.OBJECT;
    }
}
