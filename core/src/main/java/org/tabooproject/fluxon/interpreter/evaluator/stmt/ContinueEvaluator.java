package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.ContinueException;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.statement.ContinueStatement;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.ATHROW;

public class ContinueEvaluator extends StatementEvaluator<ContinueStatement> {

    @Override
    public StatementType getType() {
        return StatementType.CONTINUE;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ContinueStatement result) {
        throw new ContinueException();
    }

    @Override
    public Type generateBytecode(ContinueStatement result, MethodVisitor mv) {
        mv.visitInsn(ATHROW);
        return Type.VOID;
    }
}
