package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.UnaryExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.*;

public class UnaryEvaluator extends ExpressionEvaluator<UnaryExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.UNARY;
    }

    @Override
    public Type evaluate(Interpreter interpreter, UnaryExpression result) {
        Type t = interpreter.evaluate(result.getRight());
        switch (result.getOperator().getType()) {
            case NOT:
                Object right = interpreter.getResultBoxed(t);
                interpreter.resultPrimitive = isTrue(right) ? 0 : 1;
                return Type.Z;
            case MINUS:
                if (t == Type.I || t == Type.Z) {
                    interpreter.resultPrimitive = -(int) interpreter.resultPrimitive;
                    return Type.I;
                } else if (t == Type.J) {
                    interpreter.resultPrimitive = -interpreter.resultPrimitive;
                    return Type.J;
                } else if (t == Type.F) {
                    // 翻转符号位，等价于 FNEG
                    interpreter.resultPrimitive ^= 0x80000000L;
                    return Type.F;
                } else if (t == Type.D) {
                    // 翻转符号位，等价于 DNEG
                    interpreter.resultPrimitive ^= 0x8000000000000000L;
                    return Type.D;
                } else {
                    Object rightObj = interpreter.resultRef;
                    checkNumberOperand(rightObj);
                    interpreter.resultRef = negateNumber((Number) rightObj);
                    return Type.NUMBER;
                }
            default:
                throw new RuntimeException("Unknown unary operator: " + result.getOperator().getType());
        }
    }

    @Override
    public Type generateBytecode(UnaryExpression result, CodeContext ctx, MethodVisitor mv) {
        Evaluator<ParseResult> rightEval = ctx.getEvaluator(result.getRight());
        if (rightEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for operand");
        }
        // 压入操作数
        Type rightType = rightEval.generateBytecode(result.getRight(), ctx, mv);
        if (rightType == Type.VOID) {
            throw new VoidError("Void type is not allowed for unary expression operand");
        }
        // 判断操作数类型
        switch (result.getOperator().getType()) {
            case NOT:
                if (rightType == Type.BOOLEAN) {
                    mv.visitMethodInsn(INVOKEVIRTUAL, Type.BOOLEAN.getPath(), "booleanValue", "()Z", false);
                } else if (rightType != Type.Z) {
                    if (rightType.isPrimitive()) {
                        boxing(rightType, mv);
                    }
                    mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), "isTrue", "(" + Type.OBJECT + ")Z", false);
                }
                mv.visitInsn(ICONST_1);
                mv.visitInsn(IXOR);
                return Type.Z;
            case MINUS:
                if (rightType == Type.I || rightType == Type.Z) {
                    mv.visitInsn(INEG);
                    return Type.I;
                } else if (rightType == Type.J) {
                    mv.visitInsn(LNEG);
                    return Type.J;
                } else if (rightType == Type.F) {
                    mv.visitInsn(FNEG);
                    return Type.F;
                } else if (rightType == Type.D) {
                    mv.visitInsn(DNEG);
                    return Type.D;
                }
                boxing(rightType, mv);
                mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
                mv.visitMethodInsn(INVOKESTATIC, TYPE.getPath(), "negateNumber", "(" + Type.NUMBER + ")" + Type.NUMBER, false);
                return Type.NUMBER;
            default:
                throw new RuntimeException("Unknown unary operator: " + result.getOperator().getType());
        }
    }

    @Override
    public void analyzeTypes(UnaryExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getRight());
    }

    @Override
    public Type inferResultType(UnaryExpression result, TypeAnalyzer analyzer) {
        switch (result.getOperator().getType()) {
            case NOT:
                return Type.Z;
            case MINUS:
                return analyzer.inferType(result.getRight());
            default:
                return Type.OBJECT;
        }
    }
}
