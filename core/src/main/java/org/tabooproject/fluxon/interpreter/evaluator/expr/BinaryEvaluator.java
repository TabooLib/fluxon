package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
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

    private static final Type STRING_BUILDER = new Type(StringBuilder.class);

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
            // 右操作数非 primitive，尝试拆箱 right 走 primitive 路径
            Object right = interpreter.resultRef;
            Type rightPrimType = toPrimitiveType(right);
            if (rightPrimType != null) {
                return evaluatePrimitive(interpreter, lt, leftBits, rightPrimType, Type.unbox(right, rightPrimType), opType);
            }
            return evaluateBoxed(interpreter, Type.box(leftBits, lt), right, opType);
        }
        Object left = interpreter.resultRef;
        Type rt = interpreter.evaluate(result.getRight());
        if (rt.isPrimitive()) {
            // 左操作数非 primitive，尝试拆箱 left 走 primitive 路径
            long rightBits = interpreter.resultPrimitive;
            Type leftPrimType = toPrimitiveType(left);
            if (leftPrimType != null) {
                return evaluatePrimitive(interpreter, leftPrimType, Type.unbox(left, leftPrimType), rt, rightBits, opType);
            }
            return evaluateBoxed(interpreter, left, Type.box(rightBits, rt), opType);
        }
        Object right = interpreter.resultRef;
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
            case POWER:
                // 幂运算始终返回 double
                interpreter.resultPrimitive = Double.doubleToRawLongBits(Math.pow(toDouble(lt, leftBits), toDouble(rt, rightBits)));
                return Type.D;
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
        boolean res;
        // EQUAL/NOT_EQUAL 使用直接比较，正确处理 -0.0 == 0.0
        if (op == TokenType.EQUAL || op == TokenType.NOT_EQUAL) {
            boolean eq;
            if (common == Type.D) {
                eq = toDouble(lt, leftBits) == toDouble(rt, rightBits);
            } else if (common == Type.F) {
                eq = toFloat(lt, leftBits) == toFloat(rt, rightBits);
            } else if (common == Type.J) {
                eq = toLong(lt, leftBits) == toLong(rt, rightBits);
            } else {
                eq = (int) leftBits == (int) rightBits;
            }
            res = (op == TokenType.EQUAL) == eq;
        } else {
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
                default:
                    throw new RuntimeException("Unknown comparison op: " + op);
            }
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
            case POWER:         interpreter.resultRef = power(left, right); return Type.OBJECT;
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
        if (opType == TokenType.PLUS && (lt == Type.STRING || rt == Type.STRING)) {
            // 只有静态可判定的字符串拼接才跳过 Operations.add，保留 Object + Object 的动态集合/数字语义。
            emitStringConcat(lt, rt, ctx, mv);
            return Type.STRING;
        }
        // primitive 直通优化
        if (lt.isPrimitive() && rt.isPrimitive()) {
            // POWER 需要特殊处理，因为 Math.pow 需要两个 double
            if (opType == TokenType.POWER) {
                int saved = ctx.getLocalVarIndex();
                int rightSlot = ctx.allocateLocalVar(rt);
                Instructions.emitStoreLocal(mv, rt, rightSlot);
                emitWidening(lt, Type.D, mv);
                Instructions.emitLoadLocal(mv, rt, rightSlot);
                emitWidening(rt, Type.D, mv);
                ctx.restoreLocalVarIndex(saved);
                mv.visitMethodInsn(INVOKESTATIC, Type.MATH.getPath(), "pow", "(" + Type.D + Type.D + ")" + Type.D, false);
                return Type.D;
            }
            Type common = promoteType(lt, rt);
            if (lt != common || rt != common) {
                // 混合类型：存 right 到临时变量，widen left，重新加载并 widen right
                int saved = ctx.getLocalVarIndex();
                int rightSlot = ctx.allocateLocalVar(rt);
                Instructions.emitStoreLocal(mv, rt, rightSlot);
                emitWidening(lt, common, mv);
                Instructions.emitLoadLocal(mv, rt, rightSlot);
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
        Instructions.emitStoreLocal(mv, rt, rightSlot);
        boxing(lt, mv);
        Instructions.emitLoadLocal(mv, rt, rightSlot);
        boxing(rt, mv);
        ctx.restoreLocalVarIndex(saved);
        // 算术运算：根据推断的结果类型选择基本类型方法
        if (operator.arithmetic && ctx.getTypeAnalyzer() != null) {
            Type resultType = ctx.getTypeAnalyzer().inferBinaryResultType(lt, rt, opType);
            if (resultType.isPrimitive()) {
                String suffix = getPrimitiveSuffix(resultType);
                String descriptor = "(" + Type.OBJECT + Type.OBJECT + ")" + resultType.getDescriptor();
                mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), operator.name + suffix, descriptor, false);
                return resultType;
            }
        }
        mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), operator.name, operator.descriptor, false);
        if (operator.xor) {
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IXOR);
        }
        return operator.type;
    }

    private static String getPrimitiveSuffix(Type type) {
        if (type == Type.I || type == Type.Z) return "Int";
        if (type == Type.J) return "Long";
        if (type == Type.D) return "Double";
        return "";
    }

    private static void emitStringConcat(Type leftType, Type rightType, CodeContext ctx, MethodVisitor mv) {
        int saved = ctx.getLocalVarIndex();
        int rightSlot = ctx.allocateLocalVar(rightType);
        Instructions.emitStoreLocal(mv, rightType, rightSlot);
        int leftSlot = ctx.allocateLocalVar(leftType);
        Instructions.emitStoreLocal(mv, leftType, leftSlot);
        mv.visitTypeInsn(NEW, STRING_BUILDER.getPath());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, STRING_BUILDER.getPath(), "<init>", "()" + Type.VOID, false);
        Instructions.emitLoadLocal(mv, leftType, leftSlot);
        emitStringBuilderAppend(leftType, mv);
        Instructions.emitLoadLocal(mv, rightType, rightSlot);
        emitStringBuilderAppend(rightType, mv);
        mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.getPath(), "toString", "()" + Type.STRING, false);
        ctx.restoreLocalVarIndex(saved);
    }

    private static void emitStringBuilderAppend(Type type, MethodVisitor mv) {
        String descriptor;
        if (type == Type.I) descriptor = "(" + Type.I + ")" + STRING_BUILDER;
        else if (type == Type.J) descriptor = "(" + Type.J + ")" + STRING_BUILDER;
        else if (type == Type.F) descriptor = "(" + Type.F + ")" + STRING_BUILDER;
        else if (type == Type.D) descriptor = "(" + Type.D + ")" + STRING_BUILDER;
        else if (type == Type.Z) descriptor = "(" + Type.Z + ")" + STRING_BUILDER;
        else descriptor = "(" + Type.OBJECT + ")" + STRING_BUILDER;
        mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.getPath(), "append", descriptor, false);
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
            case POWER:
                // 调用方已保证两个操作数都是 double
                mv.visitMethodInsn(INVOKESTATIC, Type.MATH.getPath(), "pow", "(" + Type.D + Type.D + ")" + Type.D, false);
                return Type.D;
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

    /**
     * 尝试将 boxed Number 映射为 primitive Type，非 Number 返回 null
     */
    private static Type toPrimitiveType(Object value) {
        if (value instanceof Integer) return Type.I;
        if (value instanceof Double) return Type.D;
        if (value instanceof Long) return Type.J;
        if (value instanceof Float) return Type.F;
        return null;
    }

    @Override
    public void analyzeTypes(BinaryExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getLeft());
        analyzer.analyzeNode(result.getRight());
    }

    @Override
    public Type inferResultType(BinaryExpression result, TypeAnalyzer analyzer) {
        Type left = analyzer.inferType(result.getLeft());
        Type right = analyzer.inferType(result.getRight());
        return analyzer.inferBinaryResultType(left, right, result.getOperator().getType());
    }

    private static final Map<TokenType, BinaryOperator> OPERATORS = new EnumMap<>(TokenType.class);

    static {
        OPERATORS.put(TokenType.PLUS, new BinaryOperator("add", Type.OBJECT, true));
        OPERATORS.put(TokenType.MINUS, new BinaryOperator("subtract", Type.OBJECT, true));
        OPERATORS.put(TokenType.MULTIPLY, new BinaryOperator("multiply", Type.OBJECT, true));
        OPERATORS.put(TokenType.DIVIDE, new BinaryOperator("divide", Type.OBJECT, true));
        OPERATORS.put(TokenType.MODULO, new BinaryOperator("modulo", Type.OBJECT, true));
        OPERATORS.put(TokenType.POWER, new BinaryOperator("power", Type.OBJECT, false));
        OPERATORS.put(TokenType.GREATER, new BinaryOperator("isGreater", Type.Z));
        OPERATORS.put(TokenType.GREATER_EQUAL, new BinaryOperator("isGreaterEqual", Type.Z));
        OPERATORS.put(TokenType.LESS, new BinaryOperator("isLess", Type.Z));
        OPERATORS.put(TokenType.LESS_EQUAL, new BinaryOperator("isLessEqual", Type.Z));
        OPERATORS.put(TokenType.EQUAL, new BinaryOperator("isEqual", Type.Z));
        OPERATORS.put(TokenType.NOT_EQUAL, new BinaryOperator("isEqual", Type.Z, false, true));
    }

    private static class BinaryOperator {

        private final String name;
        private final String descriptor;
        private final boolean arithmetic;
        private final boolean xor;
        private final Type type;

        public BinaryOperator(String name, Type type) {
            this(name, type, false, false);
        }

        public BinaryOperator(String name, Type type, boolean arithmetic) {
            this(name, type, arithmetic, false);
        }

        public BinaryOperator(String name, Type type, boolean arithmetic, boolean xor) {
            this.name = name;
            this.descriptor = "(" + Type.OBJECT + Type.OBJECT + ")" + type;
            this.type = type;
            this.arithmetic = arithmetic;
            this.xor = xor;
        }
    }
}
