package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.expr.assign.AssignmentTargetHandler;
import org.tabooproject.fluxon.interpreter.evaluator.expr.assign.IdentifierAssignHandler;
import org.tabooproject.fluxon.interpreter.evaluator.expr.assign.IndexAccessAssignHandler;
import org.tabooproject.fluxon.interpreter.evaluator.expr.assign.MemberAccessAssignHandler;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.IndexAccessExpression;
import org.tabooproject.fluxon.parser.expression.MemberAccessExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.collection.ImmutableMap;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

/**
 * 赋值表达式求值器
 *
 * @author sky
 */
public class AssignmentEvaluator extends ExpressionEvaluator<AssignExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.ASSIGNMENT;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Type evaluate(Interpreter interpreter, AssignExpression result) {
        ParseResult target = result.getTarget();
        Type vt = interpreter.evaluate(result.getValue());
        if (interpreter.hasReturn) return Type.VOID;
        AssignmentTargetHandler handler = HANDLERS.get(target.getClass());
        handler.assign(interpreter, result, target, vt, result.getOperator().getType());
        interpreter.resultRef = null;
        return Type.VOID;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Type generateBytecode(AssignExpression result, CodeContext ctx, MethodVisitor mv) {
        ParseResult target = result.getTarget();
        Evaluator<ParseResult> valueEval = requireEvaluator(ctx, result.getValue(), "value");
        AssignmentTargetHandler handler = HANDLERS.get(target.getClass());
        handler.generateBytecode(result, target, valueEval, ctx, mv);
        return VOID;
    }

    @Override
    public void analyzeTypes(AssignExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getValue());
        int position = result.getPosition();
        if (position >= 0) {
            Type valueType;
            if (result.getOperator().getType() != TokenType.ASSIGN) {
                Type currentType = analyzer.getVariableType(position);
                Type rightType = analyzer.inferType(result.getValue());
                valueType = analyzer.inferBinaryResultType(currentType, rightType, result.getOperator().getType());
            } else {
                valueType = analyzer.inferType(result.getValue());
            }
            analyzer.recordType(position, valueType);
        } else if (result.getTarget() instanceof Identifier) {
            // root 变量不能变成本地缓存，但可记录脚本内数值类型供字节码选择 primitive 运算。
            Identifier target = (Identifier) result.getTarget();
            Type valueType;
            if (result.getOperator().getType() != TokenType.ASSIGN) {
                Type currentType = analyzer.getRootVariableType(target.getValue());
                Type rightType = analyzer.inferType(result.getValue());
                valueType = analyzer.inferBinaryResultType(currentType, rightType, result.getOperator().getType());
            } else {
                valueType = analyzer.inferType(result.getValue());
            }
            analyzer.recordRootType(target.getValue(), valueType);
        }
        recordConstant(result, analyzer);
    }

    @Override
    public Type inferResultType(AssignExpression result, TypeAnalyzer analyzer) {
        return analyzer.inferType(result.getValue());
    }

    private void recordConstant(AssignExpression result, TypeAnalyzer analyzer) {
        if (!(result.getTarget() instanceof Identifier)) {
            return;
        }
        Identifier target = (Identifier) result.getTarget();
        Object value = result.getOperator().getType() == TokenType.ASSIGN ? analyzer.inferConstant(result.getValue()) : null;
        int position = result.getPosition();
        if (position >= 0) {
            analyzer.recordLocalConstant(position, value);
        } else {
            analyzer.recordRootConstant(target.getValue(), value);
        }
    }

    public static final String SET_LOCAL_REF = "(" + I + OBJECT + ")" + VOID;
    public static final String GET_LOCAL_REF = "(" + I + ")" + OBJECT;
    public static final String SET_ROOT_VARIABLE = "(" + STRING + OBJECT + ")" + VOID;
    public static final String GET_ROOT_VARIABLE = "(" + STRING + ")" + OBJECT;

    private static final Map<TokenType, String> OPERATORS = new HashMap<>();
    private static final Map<Class<?>, AssignmentTargetHandler<?>> HANDLERS = new HashMap<>();

    static {
        OPERATORS.put(TokenType.PLUS_ASSIGN, "add");
        OPERATORS.put(TokenType.MINUS_ASSIGN, "subtract");
        OPERATORS.put(TokenType.MULTIPLY_ASSIGN, "multiply");
        OPERATORS.put(TokenType.DIVIDE_ASSIGN, "divide");
        OPERATORS.put(TokenType.MODULO_ASSIGN, "modulo");
        HANDLERS.put(Identifier.class, new IdentifierAssignHandler());
        HANDLERS.put(IndexAccessExpression.class, new IndexAccessAssignHandler());
        HANDLERS.put(MemberAccessExpression.class, new MemberAccessAssignHandler());
    }

    // 基本类型转换指令查表 [from][to]: I=0, J=1, F=2, D=3
    private static final int[][] CONVERT_INSNS = {
            //     I    J    F    D
            /* I */ {0, I2L, I2F, I2D},
            /* J */ {L2I, 0, L2F, L2D},
            /* F */ {F2I, F2L, 0, F2D},
            /* D */ {D2I, D2L, D2F, 0},
    };

    public static Object getLocalBoxed(Environment env, int position, Type varType) {
        switch (varType.getDescriptor().charAt(0)) {
            case 'I': case 'Z': return env.getLocalInt(position);
            case 'J': return env.getLocalLong(position);
            case 'F': return env.getLocalFloat(position);
            case 'D': return env.getLocalDouble(position);
            default: return env.getLocalRef(position);
        }
    }

    public static void setLocalFromBits(Environment env, int position, Type type, long bits) {
        switch (type.getDescriptor().charAt(0)) {
            case 'I': case 'Z': env.setLocalInt(position, (int) bits); break;
            case 'J': env.setLocalLong(position, bits); break;
            case 'F': env.setLocalFloat(position, Float.intBitsToFloat((int) bits)); break;
            case 'D': env.setLocalDouble(position, Double.longBitsToDouble(bits)); break;
        }
    }

    public static void setLocalFromBoxed(Environment env, int position, Type varType, Object value) {
        switch (varType.getDescriptor().charAt(0)) {
            case 'I': case 'Z': env.setLocalInt(position, ((Number) value).intValue()); break;
            case 'J': env.setLocalLong(position, ((Number) value).longValue()); break;
            case 'F': env.setLocalFloat(position, ((Number) value).floatValue()); break;
            case 'D': env.setLocalDouble(position, ((Number) value).doubleValue()); break;
            default: env.setLocalRef(position, value); break;
        }
    }

    /**
     * primitive → 不同 primitive 的直接转换，避免装箱
     */
    public static void setLocalPrimitiveConverted(Environment env, int pos, Type target, Type source, long bits) {
        double v;
        if (source == Type.I || source == Type.Z) v = (int) bits;
        else if (source == Type.J) v = (double) bits;
        else if (source == Type.F) v = Float.intBitsToFloat((int) bits);
        else if (source == Type.D) v = Double.longBitsToDouble(bits);
        else return;
        if (target == Type.I || target == Type.Z) env.setLocalInt(pos, (int) v);
        else if (target == Type.J) env.setLocalLong(pos, (long) v);
        else if (target == Type.F) env.setLocalFloat(pos, (float) v);
        else if (target == Type.D) env.setLocalDouble(pos, v);
    }

    public static Object applyCompoundOperation(Object current, Object value, TokenType operator) {
        switch (operator) {
            case PLUS_ASSIGN: return add(current, value);
            case MINUS_ASSIGN: return subtract(current, value);
            case MULTIPLY_ASSIGN: return multiply(current, value);
            case DIVIDE_ASSIGN: return divide(current, value);
            case MODULO_ASSIGN: return modulo(current, value);
            default: throw new RuntimeException("Unknown compound assignment operator: " + operator);
        }
    }

    public static Evaluator<ParseResult> requireEvaluator(CodeContext ctx, ParseResult expr, String context) {
        Evaluator<ParseResult> eval = ctx.getEvaluator(expr);
        if (eval == null) throw new EvaluatorNotFoundError("No evaluator found for " + context);
        return eval;
    }

    public static void box(Type type, MethodVisitor mv) {
        boxing(type, mv);
    }

    public static void unbox(Type type, MethodVisitor mv) {
        ExpressionEvaluator.emitUnbox(type, mv);
    }

    public static void generateBoxedValue(Evaluator<ParseResult> eval, ParseResult expr, CodeContext ctx, MethodVisitor mv) {
        Type t = eval.generateBytecode(expr, ctx, mv);
        if (t == VOID) throw new VoidError("Void type is not allowed for assignment value");
        boxing(t, mv);
    }

    public static void generateCompoundOperation(AssignExpression result, Evaluator<ParseResult> valueEval, TokenType operatorType, CodeContext ctx, MethodVisitor mv) {
        generateBoxedValue(valueEval, result.getValue(), ctx, mv);
        String operatorName = OPERATORS.get(operatorType);
        if (operatorName == null) {
            throw new RuntimeException("Unknown compound assignment operator: " + operatorType);
        }
        mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), operatorName, "(" + OBJECT + OBJECT + ")" + OBJECT, false);
    }

    private static int typeIndex(Type t) {
        switch (t.getDescriptor().charAt(0)) {
            case 'I': case 'Z': return 0;
            case 'J': return 1;
            case 'F': return 2;
            case 'D': return 3;
            default: return -1;
        }
    }

    public static void emitConvert(Type from, Type to, MethodVisitor mv) {
        if (from.equals(to)) return;
        if (!from.isPrimitive()) {
            ExpressionEvaluator.emitUnbox(to, mv);
            return;
        }
        int fi = typeIndex(from), ti = typeIndex(to);
        if (fi >= 0 && ti >= 0) {
            int insn = CONVERT_INSNS[fi][ti];
            if (insn != 0) mv.visitInsn(insn);
        }
    }
}
