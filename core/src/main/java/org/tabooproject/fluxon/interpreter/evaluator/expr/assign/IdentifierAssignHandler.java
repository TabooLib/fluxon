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
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.VoidError;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.interpreter.evaluator.expr.AssignmentEvaluator.*;
import static org.tabooproject.fluxon.runtime.Type.*;

/**
 * 标识符赋值处理器（局部变量 + 根变量）
 *
 * @author sky
 */
public class IdentifierAssignHandler implements AssignmentTargetHandler<Identifier> {

    @Override
    public void assign(Interpreter interpreter, AssignExpression expr, Identifier target, Type vt, TokenType op) {
        int position = expr.getPosition();
        // Env-free 路径：读写 FunctionContext 数组（全部使用 boxed Object）
        FunctionContext<?> ctx = interpreter.activeFunctionContext;
        if (position >= 0 && ctx != null) {
            Object value = interpreter.getResultBoxed(vt);
            if (op != TokenType.ASSIGN) {
                Object current = ctx.getLocal(position);
                value = applyCompoundOperation(current, value, op);
            }
            ctx.setLocal(position, value);
            return;
        }
        Environment env = interpreter.getEnvironment();
        if (position >= 0) {
            Type varType = env.getVariableType(position);
            if (op != TokenType.ASSIGN) {
                Object value = interpreter.getResultBoxed(vt);
                Object current = getLocalBoxed(env, position, varType);
                Object newValue = applyCompoundOperation(current, value, op);
                setLocalFromBoxed(env, position, varType, newValue);
            } else if (vt == varType && vt.isPrimitive()) {
                setLocalFromBits(env, position, vt, interpreter.resultPrimitive);
            } else if (vt.isPrimitive()) {
                if (varType.isPrimitive()) {
                    setLocalPrimitiveConverted(env, position, varType, vt, interpreter.resultPrimitive);
                } else {
                    env.setLocalRef(position, Type.box(interpreter.resultPrimitive, vt));
                }
            } else if (varType.isPrimitive()) {
                setLocalFromBoxed(env, position, varType, interpreter.resultRef);
            } else {
                env.setLocalRef(position, interpreter.resultRef);
            }
        } else {
            Object value = interpreter.getResultBoxed(vt);
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
        if (position >= 0) {
            if (ctx.isEnvFreeMode()) {
                generateEnvFreeLocal(expr, valueEval, ctx, mv, position, op);
            } else {
                generateEnvLocal(expr, valueEval, ctx, mv, position, op);
            }
        } else {
            generateRootVariable(expr, target, valueEval, ctx, mv, op);
        }
    }

    /**
     * Env-free 模式：读写 JVM 局部变量
     */
    private void generateEnvFreeLocal(AssignExpression expr, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv, int position, TokenType op) {
        Type varType = ctx.getVariableType(position);
        int jvmSlot = ctx.getJvmSlot(position);
        if (op == TokenType.ASSIGN) {
            Type vt = valueEval.generateBytecode(expr.getValue(), ctx, mv);
            if (vt == VOID) throw new VoidError("Void type is not allowed for assignment value");
            if (varType.isPrimitive()) {
                emitConvert(vt, varType, mv);
                emitJvmStore(varType, jvmSlot, mv);
            } else {
                box(vt, mv);
                mv.visitVarInsn(ASTORE, jvmSlot);
            }
            return;
        }
        // 复合赋值：从 JVM 局部变量加载 → 运算 → 存回
        if (varType.isPrimitive()) {
            emitJvmLoad(varType, jvmSlot, mv);
            box(varType, mv);
            generateCompoundOperation(expr, valueEval, op, ctx, mv);
            unbox(varType, mv);
            emitJvmStore(varType, jvmSlot, mv);
        } else {
            mv.visitVarInsn(ALOAD, jvmSlot);
            generateCompoundOperation(expr, valueEval, op, ctx, mv);
            mv.visitVarInsn(ASTORE, jvmSlot);
        }
    }

    /**
     * 传统模式：读写 Environment 局部变量槽位
     */
    private void generateEnvLocal(AssignExpression expr, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv, int position, TokenType op) {
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
            return;
        }
        // 复合赋值：从 Environment 加载 → 运算 → 存回
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
            mv.visitInsn(DUP);
            mv.visitLdcInsn(position);
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getLocalRef", GET_LOCAL_REF, false);
            generateCompoundOperation(expr, valueEval, op, ctx, mv);
            mv.visitLdcInsn(position);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalRef", SET_LOCAL_REF, false);
        }
    }

    /**
     * 根变量赋值：通过 Environment.getRootVariable/setRootVariable
     */
    private void generateRootVariable(AssignExpression expr, Identifier target, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv, TokenType op) {
        String name = target.getValue();
        Instructions.loadEnvironment(mv, ctx);
        if (op == TokenType.ASSIGN) {
            mv.visitLdcInsn(name);
            generateBoxedValue(valueEval, expr.getValue(), ctx, mv);
        } else {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(name);
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getRootVariable", GET_ROOT_VARIABLE, false);
            generateCompoundOperation(expr, valueEval, op, ctx, mv);
            mv.visitLdcInsn(name);
            mv.visitInsn(SWAP);
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setRootVariable", SET_ROOT_VARIABLE, false);
    }

    private static void emitJvmLoad(Type type, int slot, MethodVisitor mv) {
        if (type == Type.I || type == Type.Z) mv.visitVarInsn(ILOAD, slot);
        else if (type == Type.J) mv.visitVarInsn(LLOAD, slot);
        else if (type == Type.D) mv.visitVarInsn(DLOAD, slot);
        else if (type == Type.F) mv.visitVarInsn(FLOAD, slot);
        else mv.visitVarInsn(ALOAD, slot);
    }

    private static void emitJvmStore(Type type, int slot, MethodVisitor mv) {
        if (type == Type.I || type == Type.Z) mv.visitVarInsn(ISTORE, slot);
        else if (type == Type.J) mv.visitVarInsn(LSTORE, slot);
        else if (type == Type.D) mv.visitVarInsn(DSTORE, slot);
        else if (type == Type.F) mv.visitVarInsn(FSTORE, slot);
        else mv.visitVarInsn(ASTORE, slot);
    }
}
