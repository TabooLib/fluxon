package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
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
import org.tabooproject.fluxon.runtime.DirectBinding;
import org.tabooproject.fluxon.runtime.OperatorOverloadRegistry;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.collection.ImmutableMap;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getReturnType;
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
                // 原地运算符重载保留左值类型，避免 Collection += value 退化成 Object。
                if (OperatorOverloadRegistry.has(result.getOperator().getType(), currentType, rightType)) {
                    valueType = currentType;
                } else {
                    valueType = analyzer.inferBinaryResultType(currentType, rightType, result.getOperator().getType());
                }
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
                // root 变量同样保持原地重载的左值类型，后续上下文调用才能继续直连扩展函数。
                if (OperatorOverloadRegistry.has(result.getOperator().getType(), currentType, rightType)) {
                    valueType = currentType;
                } else {
                    valueType = analyzer.inferBinaryResultType(currentType, rightType, result.getOperator().getType());
                }
            } else {
                valueType = analyzer.inferType(result.getValue());
            }
            analyzer.recordRootType(target.getValue(), valueType);
        }
        recordConstant(result, analyzer);
    }

    @Override
    public Type inferResultType(AssignExpression result, TypeAnalyzer analyzer) {
        return VOID;
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
        OPERATORS.put(TokenType.PLUS_ASSIGN, "addAssign");
        OPERATORS.put(TokenType.MINUS_ASSIGN, "subtractAssign");
        OPERATORS.put(TokenType.MULTIPLY_ASSIGN, "multiplyAssign");
        OPERATORS.put(TokenType.DIVIDE_ASSIGN, "divideAssign");
        OPERATORS.put(TokenType.MODULO_ASSIGN, "moduloAssign");
        HANDLERS.put(Identifier.class, new IdentifierAssignHandler());
        HANDLERS.put(IndexAccessExpression.class, new IndexAccessAssignHandler());
        HANDLERS.put(MemberAccessExpression.class, new MemberAccessAssignHandler());
    }

    public static Object applyCompoundOperation(Object current, Object value, TokenType operator) {
        switch (operator) {
            case PLUS_ASSIGN: return addAssign(current, value);
            case MINUS_ASSIGN: return subtractAssign(current, value);
            case MULTIPLY_ASSIGN: return multiplyAssign(current, value);
            case DIVIDE_ASSIGN: return divideAssign(current, value);
            case MODULO_ASSIGN: return moduloAssign(current, value);
            default: throw new RuntimeException("Unknown compound assignment operator: " + operator);
        }
    }

    public static Evaluator<ParseResult> requireEvaluator(CodeContext ctx, ParseResult expr, String context) {
        Evaluator<ParseResult> eval = ctx.getEvaluator(expr);
        if (eval == null) throw new EvaluatorNotFoundError("No evaluator found for " + context);
        return eval;
    }

    public static void generateBoxedValue(Evaluator<ParseResult> eval, ParseResult expr, CodeContext ctx, MethodVisitor mv) {
        Type t = eval.generateBytecode(expr, ctx, mv);
        if (t == VOID) throw new VoidError("Void type is not allowed for assignment value");
        Instructions.emitBox(mv, t);
    }

    public static void generateCompoundOperation(AssignExpression result, Evaluator<ParseResult> valueEval, TokenType operatorType, CodeContext ctx, MethodVisitor mv) {
        generateCompoundOperation(result, valueEval, operatorType, ctx, mv, Type.OBJECT);
    }

    public static void generateCompoundOperation(AssignExpression result, Evaluator<ParseResult> valueEval, TokenType operatorType, CodeContext ctx, MethodVisitor mv, Type currentType) {
        Type rightType = ctx.getTypeAnalyzer() != null ? valueEval.inferResultType(result.getValue(), ctx.getTypeAnalyzer()) : Type.OBJECT;
        OperatorOverloadRegistry.Entry overloaded = OperatorOverloadRegistry.resolve(operatorType, currentType, rightType);
        if (overloaded != null) {
            emitOperatorOverload(result, valueEval, ctx, mv, overloaded);
            return;
        }
        generateBoxedValue(valueEval, result.getValue(), ctx, mv);
        String operatorName = OPERATORS.get(operatorType);
        if (operatorName == null) {
            throw new RuntimeException("Unknown compound assignment operator: " + operatorType);
        }
        mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), operatorName, "(" + OBJECT + OBJECT + ")" + OBJECT, false);
    }

    private static void emitOperatorOverload(AssignExpression result, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv, OperatorOverloadRegistry.Entry overloaded) {
        DirectBinding binding = overloaded.getBinding();
        Class<?> targetClass = overloaded.getTarget();
        if (targetClass != Object.class) {
            mv.visitTypeInsn(CHECKCAST, getInternalName(targetClass));
        }
        Type actualRightType = valueEval.generateBytecode(result.getValue(), ctx, mv);
        if (actualRightType == VOID) throw new VoidError("Void type is not allowed for assignment value");
        Type expectedRightType = Type.fromClass(overloaded.getRight());
        Instructions.emitArgumentConversion(actualRightType, expectedRightType, mv);
        if (!actualRightType.isPrimitive() && overloaded.getRight() != Object.class) {
            mv.visitTypeInsn(CHECKCAST, getInternalName(overloaded.getRight()));
        }
        mv.visitMethodInsn(INVOKESTATIC, binding.getOwner(), binding.getMethod(), binding.getDescriptor(), false);
        Type returnType = Type.fromClass(getReturnClass(binding.getDescriptor()));
        if (returnType == VOID) {
            mv.visitInsn(ACONST_NULL);
            return;
        }
        if (returnType.isPrimitive()) {
            Instructions.emitBox(mv, returnType);
        }
    }

    private static Class<?> getReturnClass(String descriptor) {
        org.objectweb.asm.Type returnType = getReturnType(descriptor);
        switch (returnType.getSort()) {
            case org.objectweb.asm.Type.VOID: return void.class;
            case org.objectweb.asm.Type.INT: return int.class;
            case org.objectweb.asm.Type.LONG: return long.class;
            case org.objectweb.asm.Type.FLOAT: return float.class;
            case org.objectweb.asm.Type.DOUBLE: return double.class;
            case org.objectweb.asm.Type.BOOLEAN: return boolean.class;
            default: return Object.class;
        }
    }

}
