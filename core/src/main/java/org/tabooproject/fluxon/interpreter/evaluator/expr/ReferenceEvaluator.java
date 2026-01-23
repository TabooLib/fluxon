package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public class ReferenceEvaluator extends ExpressionEvaluator<ReferenceExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.REFERENCE;
    }

    @Override
    public Type evaluate(Interpreter interpreter, ReferenceExpression result) {
        interpreter.resultRef = Intrinsics.getVariableOrFunction(interpreter.getEnvironment(), result.getIdentifier().getValue(), result.isOptional(), result.getPosition());
        return Type.OBJECT;
    }

    @Override
    public Type generateBytecode(ReferenceExpression result, CodeContext ctx, MethodVisitor mv) {
        int position = result.getPosition();
        Instructions.loadEnvironment(mv, ctx);
        if (position >= 0) {
            // 局部变量直接索引访问: env.getLocalRef(index)
            mv.visitLdcInsn(position);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Environment.TYPE.getPath(),
                    "getLocalRef",
                    "(" + Type.I + ")" + Type.OBJECT,
                    false
            );
        } else {
            // 根变量/函数查找: Intrinsics.getVariableOrFunction(env, name, optional, -1)
            mv.visitLdcInsn(result.getIdentifier().getValue());
            mv.visitInsn(result.isOptional() ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
            mv.visitLdcInsn(-1);
            mv.visitMethodInsn(INVOKESTATIC,
                    Intrinsics.TYPE.getPath(),
                    "getVariableOrFunction",
                    "(" + Environment.TYPE + Type.STRING + Type.Z + Type.I + ")" + Type.OBJECT,
                    false
            );
        }
        return Type.OBJECT;
    }
}
