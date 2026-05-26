package org.tabooproject.fluxon.interpreter.evaluator.expr.assign;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.runtime.CaptureCell;
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
                Object current = env.getLocalBoxed(position, varType);
                Object newValue = applyCompoundOperation(current, value, op);
                env.setLocalFromObject(position, varType, newValue);
            } else if (vt == varType && vt.isPrimitive()) {
                env.setLocalFromBits(position, vt, vt, interpreter.resultPrimitive);
            } else if (vt.isPrimitive()) {
                if (varType.isPrimitive()) {
                    env.setLocalFromBits(position, varType, vt, interpreter.resultPrimitive);
                } else {
                    env.setLocalRef(position, Type.box(interpreter.resultPrimitive, vt));
                }
            } else if (varType.isPrimitive()) {
                env.setLocalFromObject(position, varType, interpreter.resultRef);
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
            CodeContext.InlineLocalVariable cachedLocal = ctx.getInlineLocalVariable(position);
            if (cachedLocal != null) {
                generateCachedLocal(expr, valueEval, ctx, mv, cachedLocal, op);
                return;
            }
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
     * 循环局部变量缓存：循环体内先写 JVM 槽位，循环出口再统一写回 Environment。
     */
    private void generateCachedLocal(AssignExpression expr, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv, CodeContext.InlineLocalVariable cache, TokenType op) {
        generateJvmLocalAssignment(expr, valueEval, ctx, mv, cache.type, cache.slot, op);
    }

    /**
     * Env-free 模式：读写 JVM 局部变量
     */
    private void generateEnvFreeLocal(AssignExpression expr, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv, int position, TokenType op) {
        if (ctx.isLocalCapturedByChild(position) && !ctx.hasCaptureCellSlot(position)) {
            generateCapturedEnvFreeLocal(expr, valueEval, ctx, mv, position, op);
            return;
        }
        Type varType = ctx.getVariableType(position);
        int jvmSlot = ctx.getJvmSlot(position);
        generateJvmLocalAssignment(expr, valueEval, ctx, mv, varType, jvmSlot, op);
    }

    /**
     * Env-free 捕获槽位：JVM local 保存 CaptureCell，赋值只更新 cell 内容。
     */
    private void generateCapturedEnvFreeLocal(AssignExpression expr, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv, int position, TokenType op) {
        int jvmSlot = ctx.getJvmSlot(position);
        mv.visitVarInsn(ALOAD, jvmSlot);
        if (op == TokenType.ASSIGN) {
            Type vt = valueEval.generateBytecode(expr.getValue(), ctx, mv);
            if (vt == VOID) throw new VoidError("Void type is not allowed for assignment value");
            Instructions.emitBox(mv, vt);
            mv.visitMethodInsn(INVOKEVIRTUAL, CaptureCell.TYPE.getPath(), "set", "(" + OBJECT + ")V", false);
            return;
        }
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKEVIRTUAL, CaptureCell.TYPE.getPath(), "get", "()" + OBJECT, false);
        generateCompoundOperation(expr, valueEval, op, ctx, mv, Type.OBJECT);
        mv.visitMethodInsn(INVOKEVIRTUAL, CaptureCell.TYPE.getPath(), "set", "(" + OBJECT + ")V", false);
    }

    /**
     * 处理已经落到 JVM 局部槽位的变量赋值，供 env-free 与循环缓存共用。
     */
    private void generateJvmLocalAssignment(AssignExpression expr, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv, Type varType, int jvmSlot, TokenType op) {
        if (op == TokenType.ASSIGN) {
            Type vt = valueEval.generateBytecode(expr.getValue(), ctx, mv);
            if (vt == VOID) throw new VoidError("Void type is not allowed for assignment value");
            if (varType.isPrimitive()) {
                Instructions.emitConvert(vt, varType, mv);
                Instructions.emitStoreLocal(mv, varType, jvmSlot);
            } else {
                Instructions.emitBox(mv, vt);
                mv.visitVarInsn(ASTORE, jvmSlot);
            }
            return;
        }
        // 复合赋值：从 JVM 局部变量加载 → 运算 → 存回
        if (varType.isPrimitive()) {
            Type vt = ctx.getTypeAnalyzer() != null ? valueEval.inferResultType(expr.getValue(), ctx.getTypeAnalyzer()) : Type.OBJECT;
            if (vt.isPrimitive() && isNumericCompound(op, varType)) {
                // 热路径局部累加直接使用 JVM 算术指令，避免每次迭代装箱并进入 Operations。
                Instructions.emitLoadLocal(mv, varType, jvmSlot);
                vt = valueEval.generateBytecode(expr.getValue(), ctx, mv);
                if (vt == VOID) throw new VoidError("Void type is not allowed for assignment value");
                Instructions.emitConvert(vt, varType, mv);
                emitPrimitiveCompound(op, varType, mv);
                Instructions.emitStoreLocal(mv, varType, jvmSlot);
                return;
            }
            Instructions.emitLoadLocal(mv, varType, jvmSlot);
            Instructions.emitBox(mv, varType);
            generateCompoundOperation(expr, valueEval, op, ctx, mv, varType);
            Instructions.emitUnbox(mv, varType);
            Instructions.emitStoreLocal(mv, varType, jvmSlot);
        } else {
            mv.visitVarInsn(ALOAD, jvmSlot);
            generateCompoundOperation(expr, valueEval, op, ctx, mv, varType);
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
                Instructions.emitConvert(vt, varType, mv);
                Instructions.emitEnvironmentSetLocal(mv, varType);
            } else {
                Instructions.emitBox(mv, vt);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalRef", SET_LOCAL_REF, false);
            }
            return;
        }
        // 复合赋值：从 Environment 加载 → 运算 → 存回
        if (varType.isPrimitive()) {
            Type vt = ctx.getTypeAnalyzer() != null ? valueEval.inferResultType(expr.getValue(), ctx.getTypeAnalyzer()) : Type.OBJECT;
            if (vt.isPrimitive() && isNumericCompound(op, varType)) {
                // 传统 Environment 路径也保留 primitive 累加，避免 range for 内部退回 Object 运算。
                Instructions.loadEnvironment(mv, ctx);
                mv.visitLdcInsn(position);
                Instructions.loadEnvironment(mv, ctx);
                mv.visitLdcInsn(position);
                Instructions.emitEnvironmentGetLocal(mv, varType);
                vt = valueEval.generateBytecode(expr.getValue(), ctx, mv);
                if (vt == VOID) throw new VoidError("Void type is not allowed for assignment value");
                Instructions.emitConvert(vt, varType, mv);
                emitPrimitiveCompound(op, varType, mv);
                Instructions.emitEnvironmentSetLocal(mv, varType);
                return;
            }
            Instructions.loadEnvironment(mv, ctx);
            mv.visitLdcInsn(position);
            Instructions.loadEnvironment(mv, ctx);
            mv.visitLdcInsn(position);
            Instructions.emitEnvironmentGetLocal(mv, varType);
            Instructions.emitBox(mv, varType);
            generateCompoundOperation(expr, valueEval, op, ctx, mv, varType);
            Instructions.emitUnbox(mv, varType);
            Instructions.emitEnvironmentSetLocal(mv, varType);
        } else {
            Instructions.loadEnvironment(mv, ctx);
            mv.visitInsn(DUP);
            mv.visitLdcInsn(position);
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getLocalRef", GET_LOCAL_REF, false);
            generateCompoundOperation(expr, valueEval, op, ctx, mv, varType);
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
        CodeContext.RootVariableCache cache = ctx.getRootVariableCache(name);
        if (cache != null) {
            generateCachedRootVariable(expr, valueEval, ctx, mv, op, cache);
            return;
        }
        Type varType = ctx.getRootVariableType(name);
        Instructions.loadEnvironment(mv, ctx);
        if (op == TokenType.ASSIGN) {
            mv.visitLdcInsn(name);
            generateBoxedValue(valueEval, expr.getValue(), ctx, mv);
        } else if (varType.isPrimitive() && isNumericCompound(op, varType)) {
            Type vt = ctx.getTypeAnalyzer() != null ? valueEval.inferResultType(expr.getValue(), ctx.getTypeAnalyzer()) : Type.OBJECT;
            if (vt.isPrimitive()) {
                // root 变量必须保持 Environment 可观察写入，只把读出后的数字运算压到 primitive 路径。
                mv.visitLdcInsn(name);
                Instructions.loadEnvironment(mv, ctx);
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getRootVariable", GET_ROOT_VARIABLE, false);
                Instructions.emitUnbox(mv, varType);
                vt = valueEval.generateBytecode(expr.getValue(), ctx, mv);
                if (vt == VOID) throw new VoidError("Void type is not allowed for assignment value");
                Instructions.emitConvert(vt, varType, mv);
                emitPrimitiveCompound(op, varType, mv);
                Instructions.emitBox(mv, varType);
            } else {
                generateRootCompoundFallback(expr, valueEval, ctx, mv, op, name, varType);
            }
        } else {
            generateRootCompoundFallback(expr, valueEval, ctx, mv, op, name, varType);
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setRootVariable", SET_ROOT_VARIABLE, false);
    }

    /**
     * root 变量复合赋值兜底路径：保留栈顶 Environment，生成 setRootVariable 所需的 name/value。
     */
    private void generateRootCompoundFallback(AssignExpression expr, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv, TokenType op, String name, Type varType) {
        mv.visitInsn(DUP);
        mv.visitLdcInsn(name);
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "getRootVariable", GET_ROOT_VARIABLE, false);
        generateCompoundOperation(expr, valueEval, op, ctx, mv, varType);
        mv.visitLdcInsn(name);
        mv.visitInsn(SWAP);
    }

    /**
     * root 缓存变量赋值：循环体内先写 JVM 槽位，循环出口再统一写回 Environment。
     */
    private void generateCachedRootVariable(AssignExpression expr, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv, TokenType op, CodeContext.RootVariableCache cache) {
        Type varType = cache.type;
        if (op == TokenType.ASSIGN) {
            Type vt = valueEval.generateBytecode(expr.getValue(), ctx, mv);
            if (vt == VOID) throw new VoidError("Void type is not allowed for assignment value");
            Instructions.emitConvert(vt, varType, mv);
            Instructions.emitStoreLocal(mv, varType, cache.slot);
            return;
        }
        if (varType.isPrimitive() && isNumericCompound(op, varType)) {
            Type vt = ctx.getTypeAnalyzer() != null ? valueEval.inferResultType(expr.getValue(), ctx.getTypeAnalyzer()) : Type.OBJECT;
            if (vt.isPrimitive()) {
                Instructions.emitLoadLocal(mv, varType, cache.slot);
                vt = valueEval.generateBytecode(expr.getValue(), ctx, mv);
                if (vt == VOID) throw new VoidError("Void type is not allowed for assignment value");
                Instructions.emitConvert(vt, varType, mv);
                emitPrimitiveCompound(op, varType, mv);
                Instructions.emitStoreLocal(mv, varType, cache.slot);
                return;
            }
        }
        Instructions.emitLoadLocal(mv, varType, cache.slot);
        Instructions.emitBox(mv, varType);
        generateCompoundOperation(expr, valueEval, op, ctx, mv, varType);
        Instructions.emitUnbox(mv, varType);
        Instructions.emitStoreLocal(mv, varType, cache.slot);
    }

    private static boolean isNumericCompound(TokenType op, Type type) {
        if (type == Type.Z) return false;
        return op == TokenType.PLUS_ASSIGN
                || op == TokenType.MINUS_ASSIGN
                || op == TokenType.MULTIPLY_ASSIGN
                || op == TokenType.DIVIDE_ASSIGN
                || op == TokenType.MODULO_ASSIGN;
    }

    private static void emitPrimitiveCompound(TokenType op, Type type, MethodVisitor mv) {
        int typeOffset;
        if (type == Type.I) typeOffset = 0;
        else if (type == Type.J) typeOffset = 1;
        else if (type == Type.F) typeOffset = 2;
        else typeOffset = 3;
        int opOffset;
        switch (op) {
            case PLUS_ASSIGN:
                opOffset = 0;
                break;
            case MINUS_ASSIGN:
                opOffset = 1;
                break;
            case MULTIPLY_ASSIGN:
                opOffset = 2;
                break;
            case DIVIDE_ASSIGN:
                opOffset = 3;
                break;
            case MODULO_ASSIGN:
                opOffset = 4;
                break;
            default:
                throw new RuntimeException("Unknown compound assignment operator: " + op);
        }
        mv.visitInsn(IADD + opOffset * 4 + typeOffset);
    }
}
