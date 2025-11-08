package org.tabooproject.fluxon.interpreter.evaluator.stmt;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.ReturnValue;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.statement.ReturnStatement;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;

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
        // 通过抛出异常跳出函数执行
        throw new ReturnValue(value);
    }

    @Override
    public Type generateBytecode(ReturnStatement result, CodeContext ctx, MethodVisitor mv) {
        // 处理返回值
        if (result.getValue() != null) {
            // 有返回值：计算表达式并返回
            Evaluator<ParseResult> valueEval = ctx.getEvaluator(result.getValue());
            if (valueEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for return value expression");
            }
            // 生成返回值表达式的字节码
            Type valueType = valueEval.generateBytecode(result.getValue(), ctx, mv);
            // 如果表达式返回 void，则返回 null
            if (valueType == Type.VOID) {
                mv.visitInsn(ACONST_NULL);
            }
            // 返回对象引用
            mv.visitInsn(ARETURN);
        } else {
            // 无返回值：返回 null
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        }
        // Return 语句本身不返回值
        return Type.VOID;
    }
}
