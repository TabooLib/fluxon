package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.BreakException;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.statement.BreakStatement;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.ATHROW;

public class BreakEvaluator extends StatementEvaluator<BreakStatement> {

    @Override
    public StatementType getType() {
        return StatementType.BREAK;
    }

    @Override
    public Object evaluate(Interpreter interpreter, BreakStatement result) {
        throw new BreakException();
    }

    @Override
    public Type generateBytecode(BreakStatement result, CodeContext ctx, MethodVisitor mv) {
        // 生成字节码
        mv.visitInsn(ATHROW);
        return Type.VOID;
    }
}
