package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;

import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.statement.ReturnStatement;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;

import static org.objectweb.asm.Opcodes.*;

public class ReturnEvaluator extends StatementEvaluator<ReturnStatement> {

    @Override
    public StatementType getType() {
        return StatementType.RETURN;
    }

    @Override
    public Type evaluate(Interpreter interpreter, ReturnStatement result) {
        Object value = null;
        if (result.getValue() != null) {
            Type t = interpreter.evaluate(result.getValue());
            value = interpreter.getResultBoxed(t);
        }
        interpreter.hasReturn = true;
        interpreter.returnValue = value;
        return Type.VOID;
    }

    @Override
    public Type generateBytecode(ReturnStatement result, CodeContext ctx, MethodVisitor mv) {
        Class<?> expectedReturnType = ctx.getExpectedReturnType();
        if (result.getValue() != null) {
            Evaluator<ParseResult> valueEval = ctx.getEvaluator(result.getValue());
            if (valueEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for return value expression");
            }
            Type valueType = valueEval.generateBytecode(result.getValue(), ctx, mv);
            if (expectedReturnType == null) {
                // Fluxon 函数体：通过 context 写入返回值
                if (valueType == Type.VOID) {
                    mv.visitInsn(RETURN);
                } else if (valueType.isPrimitive()) {
                    mv.visitVarInsn(ALOAD, 1);
                    if (valueType == Type.D || valueType == Type.J) {
                        mv.visitInsn(DUP_X2);
                        mv.visitInsn(POP);
                    } else {
                        mv.visitInsn(SWAP);
                    }
                    Instructions.emitSetReturnPrimitive(mv, valueType);
                    mv.visitInsn(RETURN);
                } else {
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitInsn(SWAP);
                    mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "setReturnRef", "(" + Type.OBJECT + ")V", false);
                    mv.visitInsn(RETURN);
                }
            } else {
                if (valueType == Type.VOID) {
                    mv.visitInsn(ACONST_NULL);
                    valueType = Type.OBJECT;
                } else if (valueType.isPrimitive() && !expectedReturnType.isPrimitive()) {
                    boxing(valueType, mv);
                    valueType = Type.OBJECT;
                }
                Instructions.emitReturn(mv, expectedReturnType, valueType);
            }
        } else {
            if (expectedReturnType == null || expectedReturnType == void.class) {
                mv.visitInsn(RETURN);
            } else {
                mv.visitInsn(ACONST_NULL);
                mv.visitInsn(ARETURN);
            }
        }
        return Type.VOID;
    }

    @Override
    public void analyzeTypes(ReturnStatement result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getValue());
    }
}
