package org.tabooproject.fluxon.interpreter.evaluator.expr.assign;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.ReferenceEvaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.VoidError;

import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.tabooproject.fluxon.interpreter.evaluator.expr.AssignmentEvaluator.*;
import static org.tabooproject.fluxon.runtime.Type.*;

/**
 * 标识符赋值处理器（局部变量 + 根变量）
 *
 * @author sky
 */
public class IdentifierAssignHandler implements AssignmentTargetHandler<Identifier> {

    @Override
    public void assign(Interpreter interpreter, AssignExpression expr, Identifier target, Object value, TokenType op) {
        Environment env = interpreter.getEnvironment();
        int position = expr.getPosition();
        if (position >= 0) {
            Type varType = env.getVariableType(position);
            if (op != TokenType.ASSIGN) {
                Object current = getLocalBoxed(env, position, varType);
                Object newValue = applyCompoundOperation(current, value, op);
                setLocalFromBoxed(env, position, varType, newValue);
            } else {
                Type vt = interpreter.lastResultType;
                if (vt == varType && vt.isPrimitive()) {
                    setLocalFromBits(env, position, vt, interpreter.resultPrimitive);
                } else if (varType.isPrimitive()) {
                    setLocalFromBoxed(env, position, varType, value);
                } else {
                    env.setLocalRef(position, value);
                }
            }
        } else {
            String name = target.getValue();
            if (op != TokenType.ASSIGN) {
                value = applyCompoundOperation(env.getRootVariable(name), value, op);
            }
            env.setRootVariable(name, value);
        }
    }

    @Override
    public void generateBytecode(AssignExpression expr, Identifier target, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv) {
        int position = expr.getPosition();
        TokenType op = expr.getOperator().getType();
        String name = target.getValue();
        if (position >= 0) {
            Type varType = ctx.getVariableType(position);
            if (op == TokenType.ASSIGN) {
                Instructions.loadEnvironment(mv, ctx);
                mv.visitLdcInsn(position);
                Type vt = valueEval.generateBytecode(expr.getValue(), ctx, mv);
                if (vt == VOID) throw new VoidError("Void type is not allowed for assignment value");
                if (varType.isPrimitive()) {
                    emitConvert(vt, varType, mv);
                    ReferenceEvaluator.emitSetLocal(varType, mv);
                } else {
                    box(vt, mv);
                    mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalRef", SET_LOCAL_REF, false);
                }
            } else {
                if (varType.isPrimitive()) {
                    Instructions.loadEnvironment(mv, ctx);
                    mv.visitLdcInsn(position);
                    Instructions.loadEnvironment(mv, ctx);
                    mv.visitLdcInsn(position);
                    ReferenceEvaluator.emitGetLocal(varType, mv);
                    box(varType, mv);
                    generateCompoundOperation(expr, valueEval, op, ctx, mv);
                    unbox(varType, mv);
                    ReferenceEvaluator.emitSetLocal(varType, mv);
                } else {
                    Instructions.loadEnvironment(mv, ctx);
                    mv.visitInsn(org.objectweb.asm.Opcodes.DUP);
                    mv.visitLdcInsn(position);
                    mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getLocalRef", GET_LOCAL_REF, false);
                    generateCompoundOperation(expr, valueEval, op, ctx, mv);
                    mv.visitLdcInsn(position);
                    mv.visitInsn(org.objectweb.asm.Opcodes.SWAP);
                    mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalRef", SET_LOCAL_REF, false);
                }
            }
        } else {
            Instructions.loadEnvironment(mv, ctx);
            if (op == TokenType.ASSIGN) {
                mv.visitLdcInsn(name);
                generateBoxedValue(valueEval, expr.getValue(), ctx, mv);
            } else {
                mv.visitInsn(org.objectweb.asm.Opcodes.DUP);
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getRootVariable", GET_ROOT_VARIABLE, false);
                generateCompoundOperation(expr, valueEval, op, ctx, mv);
                mv.visitLdcInsn(name);
                mv.visitInsn(org.objectweb.asm.Opcodes.SWAP);
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setRootVariable", SET_ROOT_VARIABLE, false);
        }
    }
}
