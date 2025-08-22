package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.*;

public class ReferenceEvaluator extends ExpressionEvaluator<ReferenceExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.REFERENCE;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ReferenceExpression result) {
        return Intrinsics.getVariableOrFunction(interpreter.getEnvironment(), result.getIdentifier().getValue(), result.isOptional(), result.getPosition());
    }

    @Override
    public Type generateBytecode(ReferenceExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取环境
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());
        // 变量名
        mv.visitLdcInsn(result.getIdentifier().getValue());
        // isOptional
        mv.visitInsn(result.isOptional() ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        // 压入 index 参数
        mv.visitLdcInsn(result.getPosition());
        mv.visitMethodInsn(INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "getVariableOrFunction",
                "(" + Environment.TYPE + Type.STRING + Type.Z + Type.I + ")" + Type.OBJECT,
                false
        );
        return Type.OBJECT;
    }
}
