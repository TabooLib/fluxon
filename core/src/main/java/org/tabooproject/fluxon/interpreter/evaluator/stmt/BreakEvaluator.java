package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.BreakException;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.statement.BreakStatement;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;

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
        // 获取当前循环的 break 标签
        Label breakLabel = ctx.getCurrentBreakLabel();
        if (breakLabel == null) {
            throw new RuntimeException("Break statement not inside a loop");
        }
        // 直接跳转到 break 标签
        mv.visitJumpInsn(GOTO, breakLabel);
        return Type.VOID;
    }
}
