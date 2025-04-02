package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.ReturnValue;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.statement.ReturnStatement;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.ATHROW;

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
        // 通过抛出异常跳出函数执行
        throw new ReturnValue(value);
    }

    @Override
    public Type generateBytecode(ReturnStatement result, MethodVisitor mv) {
        // 生成字节码
        mv.visitInsn(ATHROW);
        return Type.VOID;
    }
}
