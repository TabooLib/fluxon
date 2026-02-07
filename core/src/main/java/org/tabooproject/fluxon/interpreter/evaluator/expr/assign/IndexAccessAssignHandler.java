package org.tabooproject.fluxon.interpreter.evaluator.expr.assign;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.IndexAccessExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.interpreter.evaluator.expr.AssignmentEvaluator.*;
import static org.tabooproject.fluxon.runtime.Type.*;

/**
 * 索引访问赋值处理器（数组/Map 索引赋值）
 *
 * @author sky
 */
public class IndexAccessAssignHandler implements AssignmentTargetHandler<IndexAccessExpression> {

    @Override
    public void assign(Interpreter interpreter, AssignExpression expr, IndexAccessExpression target, Type vt, TokenType op) {
        Object value = interpreter.getResultBoxed(vt);
        Type tt = interpreter.evaluate(target.getTarget());
        Object container = interpreter.getResultBoxed(tt);
        List<ParseResult> indices = target.getIndices();
        for (int i = 0; i < indices.size() - 1; i++) {
            Type it = interpreter.evaluate(indices.get(i));
            container = Intrinsics.getIndex(container, interpreter.getResultBoxed(it));
        }
        Type lit = interpreter.evaluate(indices.get(indices.size() - 1));
        Object lastIndex = interpreter.getResultBoxed(lit);
        if (op != TokenType.ASSIGN) {
            value = applyCompoundOperation(Intrinsics.getIndex(container, lastIndex), value, op);
        }
        Intrinsics.setIndex(container, lastIndex, value);
    }

    @Override
    public void generateBytecode(AssignExpression expr, IndexAccessExpression target, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv) {
        List<ParseResult> indices = target.getIndices();
        TokenType op = expr.getOperator().getType();
        Evaluator<ParseResult> targetEval = requireEvaluator(ctx, target.getTarget(), "index access target");
        Type tt = targetEval.generateBytecode(target.getTarget(), ctx, mv);
        if (tt == VOID) throw new VoidError("Void type is not allowed for index access target");
        box(tt, mv);
        // 处理多索引：前 n-1 个索引用于导航到目标容器
        for (int i = 0; i < indices.size() - 1; i++) {
            generateBoxedValue(requireEvaluator(ctx, indices.get(i), "index expression"), indices.get(i), ctx, mv);
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "getIndex", "(" + OBJECT + OBJECT + ")" + OBJECT, false);
        }
        // 最后一个索引用于赋值
        ParseResult lastIndexExpr = indices.get(indices.size() - 1);
        Evaluator<ParseResult> lastIndexEval = requireEvaluator(ctx, lastIndexExpr, "last index expression");
        if (op == TokenType.ASSIGN) {
            generateBoxedValue(lastIndexEval, lastIndexExpr, ctx, mv);
            generateBoxedValue(valueEval, expr.getValue(), ctx, mv);
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "setIndex", "(" + OBJECT + OBJECT + OBJECT + ")" + VOID, false);
        } else {
            mv.visitInsn(DUP);
            generateBoxedValue(lastIndexEval, lastIndexExpr, ctx, mv);
            mv.visitInsn(DUP_X1);
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "getIndex", "(" + OBJECT + OBJECT + ")" + OBJECT, false);
            generateCompoundOperation(expr, valueEval, op, ctx, mv);
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "setIndex", "(" + OBJECT + OBJECT + OBJECT + ")" + VOID, false);
        }
    }
}
