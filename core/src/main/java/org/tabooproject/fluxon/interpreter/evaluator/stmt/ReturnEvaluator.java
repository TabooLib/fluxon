package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.ReturnValue;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.statement.ReturnStatement;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;

import static org.objectweb.asm.Opcodes.*;

public class ReturnEvaluator extends StatementEvaluator<ReturnStatement> {

    @Override
    public StatementType getType() {
        return StatementType.RETURN;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ReturnStatement result) {
        Object value = null;
        if (result.getValue() != null) {
            value = interpreter.evaluate(result.getValue());
        }
        throw new ReturnValue(value);
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
            if (valueType == Type.VOID) {
                mv.visitInsn(ACONST_NULL);
                valueType = Type.OBJECT;
            }
            BytecodeUtils.emitReturn(mv, expectedReturnType, valueType);
        } else {
            if (expectedReturnType == void.class) {
                mv.visitInsn(RETURN);
            } else {
                mv.visitInsn(ACONST_NULL);
                mv.visitInsn(ARETURN);
            }
        }
        return Type.VOID;
    }
}
