package org.tabooproject.fluxon.interpreter.evaluator.expr.assign;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.MemberAccessExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.interpreter.evaluator.expr.AssignmentEvaluator.*;
import static org.tabooproject.fluxon.runtime.Type.*;

/**
 * 成员访问赋值处理器（字段赋值，含 safe access）
 *
 * @author sky
 */
public class MemberAccessAssignHandler implements AssignmentTargetHandler<MemberAccessExpression> {

    @Override
    public void assign(Interpreter interpreter, AssignExpression expr, MemberAccessExpression target, Type vt, TokenType op) {
        Object value = interpreter.getResultBoxed(vt);
        Type tt = interpreter.evaluate(target.getTarget());
        Object targetObj = interpreter.getResultBoxed(tt);
        if (targetObj == null) {
            if (target.isSafe()) {
                interpreter.resultRef = null;
                return;
            }
            throw new NullPointerException("Cannot set field '" + target.getMemberName() + "' on null object");
        }
        String fieldName = target.getMemberName();
        try {
            if (op != TokenType.ASSIGN) {
                Object current = ReflectionHelper.getField(targetObj, fieldName);
                value = applyCompoundOperation(current, value, op);
            }
            ReflectionHelper.setField(targetObj, fieldName, value);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void generateBytecode(AssignExpression expr, MemberAccessExpression target, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv) {
        TokenType op = expr.getOperator().getType();
        String fieldName = target.getMemberName();
        boolean safe = target.isSafe();
        Evaluator<ParseResult> targetEval = requireEvaluator(ctx, target.getTarget(), "member access target");
        Type tt = targetEval.generateBytecode(target.getTarget(), ctx, mv);
        if (tt == VOID) throw new VoidError("Void type is not allowed for member access target");
        Instructions.emitBox(mv, tt);
        Label skipLabel = null;
        Label endLabel = null;
        if (safe) {
            skipLabel = new Label();
            endLabel = new Label();
            mv.visitInsn(DUP);
            mv.visitJumpInsn(IFNULL, skipLabel);
        }
        if (op == TokenType.ASSIGN) {
            mv.visitLdcInsn(fieldName);
            generateBoxedValue(valueEval, expr.getValue(), ctx, mv);
            mv.visitMethodInsn(INVOKESTATIC, ReflectionHelper.TYPE.getPath(), "setField", "(" + OBJECT + STRING + OBJECT + ")V", false);
        } else {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(fieldName);
            mv.visitMethodInsn(INVOKESTATIC, ReflectionHelper.TYPE.getPath(), "getField", "(" + OBJECT + STRING + ")" + OBJECT, false);
            generateCompoundOperation(expr, valueEval, op, ctx, mv);
            mv.visitLdcInsn(fieldName);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESTATIC, ReflectionHelper.TYPE.getPath(), "setField", "(" + OBJECT + STRING + OBJECT + ")V", false);
        }
        if (safe) {
            mv.visitJumpInsn(GOTO, endLabel);
            mv.visitLabel(skipLabel);
            mv.visitInsn(POP);
            mv.visitLabel(endLabel);
        }
    }
}
