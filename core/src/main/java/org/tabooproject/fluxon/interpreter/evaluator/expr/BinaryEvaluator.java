package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;

import java.util.EnumMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

@SuppressWarnings("DuplicatedCode")
public class BinaryEvaluator extends ExpressionEvaluator<BinaryExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.BINARY;
    }

    @Override
    public Type evaluate(Interpreter interpreter, BinaryExpression result) {
        TokenType opType = result.getOperator().getType();
        // === 和 !== 必须装箱后做引用比较
        if (opType == TokenType.IDENTICAL || opType == TokenType.NOT_IDENTICAL) {
            Type lt = interpreter.evaluate(result.getLeft());
            Object left = interpreter.getResultBoxed(lt);
            Type rt = interpreter.evaluate(result.getRight());
            Object right = interpreter.getResultBoxed(rt);
            interpreter.resultPrimitive = ((opType == TokenType.IDENTICAL) == (left == right)) ? 1 : 0;
            return Type.Z;
        }
        // 尝试 primitive 快速路径
        Type lt = interpreter.evaluate(result.getLeft());
        if (lt.isPrimitive()) {
            long leftBits = interpreter.resultPrimitive;
            Type rt = interpreter.evaluate(result.getRight());
            if (rt.isPrimitive()) {
                long rightBits = interpreter.resultPrimitive;
                return evaluatePrimitive(interpreter, lt, leftBits, rt, rightBits, opType);
            }
            // 右操作数非 primitive，回退到装箱路径
            Object left = Type.box(leftBits, lt);
            Object right = interpreter.resultRef;
            return evaluateBoxed(interpreter, left, right, opType);
        }
        Object left = interpreter.resultRef;
        Type rt = interpreter.evaluate(result.getRight());
        Object right = interpreter.getResultBoxed(rt);
        return evaluateBoxed(interpreter, left, right, opType);
    }

    /**
     * primitive 快速路径：避免装箱，直接用 Java 运算符
     */
    private Type evaluatePrimitive(Interpreter interpreter, Type lt, long leftBits, Type rt, long rightBits, TokenType opType) {
        Type common = promoteType(lt, rt);
        switch (opType) {
            case PLUS:
            case MINUS:
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
                return evalPrimitiveArith(interpreter, common, lt, leftBits, rt, rightBits, opType);
            default:
                return evalPrimitiveCmp(interpreter, common, lt, leftBits, rt, rightBits, opType);
        }
    }

    private Type evalPrimitiveArith(Interpreter interpreter, Type common, Type lt, long leftBits, Type rt, long rightBits, TokenType op) {
        if (common == Type.D) {
            interpreter.resultPrimitive = Double.doubleToRawLongBits(arithDouble(toDouble(lt, leftBits), toDouble(rt, rightBits), op));
            return Type.D;
        } else if (common == Type.F) {
            interpreter.resultPrimitive = Float.floatToRawIntBits(arithFloat(toFloat(lt, leftBits), toFloat(rt, rightBits), op));
            return Type.F;
        } else if (common == Type.J) {
            interpreter.resultPrimitive = arithLong(toLong(lt, leftBits), toLong(rt, rightBits), op);
            return Type.J;
        } else {
            interpreter.resultPrimitive = arithInt((int) leftBits, (int) rightBits, op);
            return Type.I;
        }
    }

    // @formatter:off
    private static double arithDouble(double l, double r, TokenType op) {
        switch (op) {
            case PLUS: return l + r;  case MINUS: return l - r;  case MULTIPLY: return l * r;
            case DIVIDE: return l / r;  case MODULO: return l % r;
            default: throw new RuntimeException("Unknown arithmetic op: " + op);
        }
    }
    private static float arithFloat(float l, float r, TokenType op) {
        switch (op) {
            case PLUS: return l + r;  case MINUS: return l - r;  case MULTIPLY: return l * r;
            case DIVIDE: return l / r;  case MODULO: return l % r;
            default: throw new RuntimeException("Unknown arithmetic op: " + op);
        }
    }
    private static long arithLong(long l, long r, TokenType op) {
        switch (op) {
            case PLUS: return l + r;  case MINUS: return l - r;  case MULTIPLY: return l * r;
            case DIVIDE: return l / r;  case MODULO: return l % r;
            default: throw new RuntimeException("Unknown arithmetic op: " + op);
        }
    }
    private static int arithInt(int l, int r, TokenType op) {
        switch (op) {
            case PLUS: return l + r;  case MINUS: return l - r;  case MULTIPLY: return l * r;
            case DIVIDE: return l / r;  case MODULO: return l % r;
            default: throw new RuntimeException("Unknown arithmetic op: " + op);
        }
    }
    // @formatter:on

    private Type evalPrimitiveCmp(Interpreter interpreter, Type common, Type lt, long leftBits, Type rt, long rightBits, TokenType op) {
        int cmp;
        if (common == Type.D) {
            cmp = Double.compare(toDouble(lt, leftBits), toDouble(rt, rightBits));
        } else if (common == Type.F) {
            cmp = Float.compare(toFloat(lt, leftBits), toFloat(rt, rightBits));
        } else if (common == Type.J) {
            cmp = Long.compare(toLong(lt, leftBits), toLong(rt, rightBits));
        } else {
            cmp = Integer.compare((int) leftBits, (int) rightBits);
        }
        boolean res;
        switch (op) {
            case GREATER:
                res = cmp > 0;
                break;
            case GREATER_EQUAL:
                res = cmp >= 0;
                break;
            case LESS:
                res = cmp < 0;
                break;
            case LESS_EQUAL:
                res = cmp <= 0;
                break;
            case EQUAL:
                res = cmp == 0;
                break;
            case NOT_EQUAL:
                res = cmp != 0;
                break;
            default:
                throw new RuntimeException("Unknown comparison op: " + op);
        }
        interpreter.resultPrimitive = res ? 1 : 0;
        return Type.Z;
    }

    /**
     * 装箱回退路径
     */
    // @formatter:off
    private Type evaluateBoxed(Interpreter interpreter, Object left, Object right, TokenType opType) {
        switch (opType) {
            case PLUS:          interpreter.resultRef = add(left, right); return Type.OBJECT;
            case MINUS:         interpreter.resultRef = subtract(left, right); return Type.OBJECT;
            case MULTIPLY:      interpreter.resultRef = multiply(left, right); return Type.OBJECT;
            case DIVIDE:        interpreter.resultRef = divide(left, right); return Type.OBJECT;
            case MODULO:        interpreter.resultRef = modulo(left, right); return Type.OBJECT;
            case GREATER:       interpreter.resultPrimitive = isGreater(left, right) ? 1 : 0; return Type.Z;
            case GREATER_EQUAL: interpreter.resultPrimitive = isGreaterEqual(left, right) ? 1 : 0; return Type.Z;
            case LESS:          interpreter.resultPrimitive = isLess(left, right) ? 1 : 0; return Type.Z;
            case LESS_EQUAL:    interpreter.resultPrimitive = isLessEqual(left, right) ? 1 : 0; return Type.Z;
            case EQUAL:         interpreter.resultPrimitive = isEqual(left, right) ? 1 : 0; return Type.Z;
            case NOT_EQUAL:     interpreter.resultPrimitive = !isEqual(left, right) ? 1 : 0; return Type.Z;
            default:            throw new RuntimeException("Unknown binary operator: " + opType);
        }
    }
    // @formatter:on

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Type generateBytecode(BinaryExpression expr, CodeContext ctx, MethodVisitor mv) {
        Evaluator<ParseResult> leftEval = ctx.getEvaluator(expr.getLeft());
        Evaluator<ParseResult> rightEval = ctx.getEvaluator(expr.getRight());
        if (leftEval == null || rightEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for operands");
        }
        TokenType opType = expr.getOperator().getType();
        // 特殊处理引用比较运算符（=== 和 !==）
        if (opType == TokenType.IDENTICAL || opType == TokenType.NOT_IDENTICAL) {
            return generateIdentityComparison(expr, leftEval, rightEval, ctx, mv, opType == TokenType.NOT_IDENTICAL);
        }
        // 生成左右操作数
        Type lt = leftEval.generateBytecode(expr.getLeft(), ctx, mv);
        if (lt == Type.VOID) {
            throw new VoidError("Void type is not allowed for binary expression left operand");
        }
        Type rt = rightEval.generateBytecode(expr.getRight(), ctx, mv);
        if (rt == Type.VOID) {
            throw new VoidError("Void type is not allowed for binary expression right operand");
        }
        // primitive 直通优化
        if (lt.isPrimitive() && rt.isPrimitive()) {
            Type common = promoteType(lt, rt);
            if (lt != common || rt != common) {
                // 混合类型：存 right 到临时变量，widen left，重新加载并 widen right
                int saved = ctx.getLocalVarIndex();
                int rightSlot = ctx.allocateLocalVar(rt);
                emitStore(rt, rightSlot, mv);
                emitWidening(lt, common, mv);
                emitLoad(rt, rightSlot, mv);
                emitWidening(rt, common, mv);
                ctx.restoreLocalVarIndex(saved);
            }
            return emitPrimitiveOp(common, opType, mv);
        }
        // 回退：存 right 到临时变量，装箱 left，再加载并装箱 right
        BinaryOperator operator = OPERATORS.get(opType);
        if (operator == null) {
            throw new RuntimeException("No operator found for binary expression");
        }
        int saved = ctx.getLocalVarIndex();
        int rightSlot = ctx.allocateLocalVar(rt.isPrimitive() ? rt : Type.OBJECT);
        emitStoreAny(rt, rightSlot, mv);
        boxing(lt, mv);
        emitLoadAny(rt, rightSlot, mv);
        boxing(rt, mv);
        ctx.restoreLocalVarIndex(saved);
        mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), operator.name, operator.descriptor, false);
        if (operator.xor) {
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IXOR);
        }
        return operator.type;
    }

    /**
     * 生成引用比较字节码（=== 和 !==）
     */
    private Type generateIdentityComparison(BinaryExpression expr, Evaluator<ParseResult> leftEval, Evaluator<ParseResult> rightEval, CodeContext ctx, MethodVisitor mv, boolean negate) {
        Type lt = leftEval.generateBytecode(expr.getLeft(), ctx, mv);
        if (lt == Type.VOID) {
            throw new VoidError("Void type is not allowed for binary expression left operand");
        }
        boxing(lt, mv);
        Type rt = rightEval.generateBytecode(expr.getRight(), ctx, mv);
        if (rt == Type.VOID) {
            throw new VoidError("Void type is not allowed for binary expression right operand");
        }
        boxing(rt, mv);
        Label trueLabel = new Label();
        Label endLabel = new Label();
        mv.visitJumpInsn(negate ? IF_ACMPNE : IF_ACMPEQ, trueLabel);
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, endLabel);
        mv.visitLabel(trueLabel);
        mv.visitInsn(ICONST_1);
        mv.visitLabel(endLabel);
        return Type.Z;
    }

    /**
     * 根据 common 类型和运算符生成算术或比较指令
     */
    private static Type emitPrimitiveOp(Type common, TokenType opType, MethodVisitor mv) {
        switch (opType) {
            case PLUS:
            case MINUS:
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
                return emitArithmetic(common, opType, mv);
            default:
                return emitComparison(common, opType, mv);
        }
    }

    /**
     * 生成算术指令（IADD/LADD/FADD/DADD 等）
     * JVM opcode 布局: IADD=96, 每组 4 个 (I/L/F/D)，共 5 组 (add/sub/mul/div/rem)
     */
    private static Type emitArithmetic(Type type, TokenType op, MethodVisitor mv) {
        int typeOffset;
        if (type == Type.I) typeOffset = 0;
        else if (type == Type.J) typeOffset = 1;
        else if (type == Type.F) typeOffset = 2;
        else typeOffset = 3; // D
        int opOffset;
        switch (op) {
            case PLUS:
                opOffset = 0;
                break;
            case MINUS:
                opOffset = 1;
                break;
            case MULTIPLY:
                opOffset = 2;
                break;
            case DIVIDE:
                opOffset = 3;
                break;
            case MODULO:
                opOffset = 4;
                break;
            default:
                throw new RuntimeException("Unknown arithmetic op: " + op);
        }
        mv.visitInsn(IADD + opOffset * 4 + typeOffset);
        return type;
    }

    /**
     * 生成比较指令：对 I 使用 IF_ICMPxx，对 J/F/D 使用 xCMP + IFxx
     */
    private static Type emitComparison(Type type, TokenType op, MethodVisitor mv) {
        Label trueLabel = new Label();
        Label endLabel = new Label();
        if (type == Type.I) {
            int jumpOp;
            switch (op) {
                case GREATER:
                    jumpOp = IF_ICMPGT;
                    break;
                case GREATER_EQUAL:
                    jumpOp = IF_ICMPGE;
                    break;
                case LESS:
                    jumpOp = IF_ICMPLT;
                    break;
                case LESS_EQUAL:
                    jumpOp = IF_ICMPLE;
                    break;
                case EQUAL:
                    jumpOp = IF_ICMPEQ;
                    break;
                case NOT_EQUAL:
                    jumpOp = IF_ICMPNE;
                    break;
                default:
                    throw new RuntimeException("Unknown comparison op: " + op);
            }
            mv.visitJumpInsn(jumpOp, trueLabel);
        } else {
            // 生成比较指令
            if (type == Type.J) {
                mv.visitInsn(LCMP);
            } else if (type == Type.F) {
                // >, >=, != 用 FCMPG（NaN→1, 确保比较失败）
                mv.visitInsn(useGVariant(op) ? FCMPG : FCMPL);
            } else {
                mv.visitInsn(useGVariant(op) ? DCMPG : DCMPL);
            }
            int jumpOp;
            switch (op) {
                case GREATER:
                    jumpOp = IFGT;
                    break;
                case GREATER_EQUAL:
                    jumpOp = IFGE;
                    break;
                case LESS:
                    jumpOp = IFLT;
                    break;
                case LESS_EQUAL:
                    jumpOp = IFLE;
                    break;
                case EQUAL:
                    jumpOp = IFEQ;
                    break;
                case NOT_EQUAL:
                    jumpOp = IFNE;
                    break;
                default:
                    throw new RuntimeException("Unknown comparison op: " + op);
            }
            mv.visitJumpInsn(jumpOp, trueLabel);
        }
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, endLabel);
        mv.visitLabel(trueLabel);
        mv.visitInsn(ICONST_1);
        mv.visitLabel(endLabel);
        return Type.Z;
    }

    /**
     * 对于 >, >=, != 使用 FCMPG/DCMPG 变体以正确处理 NaN
     */
    private static boolean useGVariant(TokenType op) {
        return op == TokenType.GREATER || op == TokenType.GREATER_EQUAL || op == TokenType.NOT_EQUAL;
    }

    /**
     * 生成类型提升指令
     */
    private static void emitWidening(Type from, Type to, MethodVisitor mv) {
        if (from == to) return;
        int fromRank = rank(from);
        int toRank = rank(to);
        if (fromRank == toRank) return; // Z→I 无需指令
        switch (fromRank) {
            case 0: // I/Z →
                if (toRank == 1) mv.visitInsn(I2L);
                else if (toRank == 2) mv.visitInsn(I2F);
                else mv.visitInsn(I2D);
                break;
            case 1: // J →
                if (toRank == 2) mv.visitInsn(L2F);
                else mv.visitInsn(L2D);
                break;
            case 2: // F → D
                mv.visitInsn(F2D);
                break;
        }
    }

    private static void emitStore(Type type, int slot, MethodVisitor mv) {
        if (type == Type.J) mv.visitVarInsn(LSTORE, slot);
        else if (type == Type.D) mv.visitVarInsn(DSTORE, slot);
        else if (type == Type.F) mv.visitVarInsn(FSTORE, slot);
        else mv.visitVarInsn(ISTORE, slot); // I 或 Z
    }

    private static void emitLoad(Type type, int slot, MethodVisitor mv) {
        if (type == Type.J) mv.visitVarInsn(LLOAD, slot);
        else if (type == Type.D) mv.visitVarInsn(DLOAD, slot);
        else if (type == Type.F) mv.visitVarInsn(FLOAD, slot);
        else mv.visitVarInsn(ILOAD, slot); // I 或 Z
    }

    private static void emitStoreAny(Type type, int slot, MethodVisitor mv) {
        if (type.isPrimitive()) emitStore(type, slot, mv);
        else mv.visitVarInsn(ASTORE, slot);
    }

    private static void emitLoadAny(Type type, int slot, MethodVisitor mv) {
        if (type.isPrimitive()) emitLoad(type, slot, mv);
        else mv.visitVarInsn(ALOAD, slot);
    }

    /**
     * 二元数值提升：I→J→F→D（Z 视为 I）
     */
    private static Type promoteType(Type a, Type b) {
        int max = Math.max(rank(a), rank(b));
        switch (max) {
            case 3:
                return Type.D;
            case 2:
                return Type.F;
            case 1:
                return Type.J;
            default:
                return Type.I;
        }
    }

    private static int rank(Type t) {
        if (t == Type.D) return 3;
        if (t == Type.F) return 2;
        if (t == Type.J) return 1;
        return 0; // I 或 Z
    }

    private static long toLong(Type t, long bits) {
        if (t == Type.J) return bits;
        return (int) bits; // I/Z 符号扩展
    }

    private static float toFloat(Type t, long bits) {
        if (t == Type.F) return Float.intBitsToFloat((int) bits);
        if (t == Type.J) return (float) bits;
        return (float) (int) bits;
    }

    private static double toDouble(Type t, long bits) {
        if (t == Type.D) return Double.longBitsToDouble(bits);
        if (t == Type.F) return Float.intBitsToFloat((int) bits);
        if (t == Type.J) return (double) bits;
        return (int) bits;
    }

    private static final Map<TokenType, BinaryOperator> OPERATORS = new EnumMap<>(TokenType.class);

    static {
        OPERATORS.put(TokenType.PLUS, new BinaryOperator("add", Type.OBJECT));
        OPERATORS.put(TokenType.MINUS, new BinaryOperator("subtract", Type.OBJECT));
        OPERATORS.put(TokenType.MULTIPLY, new BinaryOperator("multiply", Type.OBJECT));
        OPERATORS.put(TokenType.DIVIDE, new BinaryOperator("divide", Type.OBJECT));
        OPERATORS.put(TokenType.MODULO, new BinaryOperator("modulo", Type.OBJECT));
        OPERATORS.put(TokenType.GREATER, new BinaryOperator("isGreater", Type.Z));
        OPERATORS.put(TokenType.GREATER_EQUAL, new BinaryOperator("isGreaterEqual", Type.Z));
        OPERATORS.put(TokenType.LESS, new BinaryOperator("isLess", Type.Z));
        OPERATORS.put(TokenType.LESS_EQUAL, new BinaryOperator("isLessEqual", Type.Z));
        OPERATORS.put(TokenType.EQUAL, new BinaryOperator("isEqual", Type.Z));
        OPERATORS.put(TokenType.NOT_EQUAL, new BinaryOperator("isEqual", Type.Z, true));
    }

    private static class BinaryOperator {

        private final String name;
        private final String descriptor;
        private final boolean xor;
        private final Type type;

        public BinaryOperator(String name, Type type) {
            this(name, type, false);
        }

        public BinaryOperator(String name, Type type, boolean xor) {
            this.name = name;
            this.descriptor = "(" + Type.OBJECT + Type.OBJECT + ")" + type;
            this.type = type;
            this.xor = xor;
        }
    }
}
