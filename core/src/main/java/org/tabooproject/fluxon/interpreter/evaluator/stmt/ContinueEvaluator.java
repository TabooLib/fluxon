package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.ContinueException;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.statement.ContinueStatement;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;

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
    public Type generateBytecode(ContinueStatement result, CodeContext ctx, MethodVisitor mv) {
        // 获取当前循环的 continue 标签
        Label continueLabel = ctx.getCurrentContinueLabel();
        if (continueLabel == null) {
            throw new RuntimeException("Continue statement not inside a loop");
        }
        // 直接跳转到 continue 标签
        mv.visitJumpInsn(GOTO, continueLabel);
        return Type.VOID;
    }
}
