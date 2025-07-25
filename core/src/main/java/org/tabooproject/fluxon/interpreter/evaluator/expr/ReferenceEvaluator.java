package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.error.FunctionNotFoundException;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class ReferenceEvaluator extends ExpressionEvaluator<ReferenceExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.REFERENCE;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ReferenceExpression result) {
        Environment environment = interpreter.getEnvironment();
        try {
            return environment.getFunction(result.getIdentifier().getValue());
        } catch (FunctionNotFoundException ignored) {
            return environment.get(result.getIdentifier().getValue());
        }
    }

    @Override
    public Type generateBytecode(ReferenceExpression result, CodeContext ctx, MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);                   // this
        mv.visitLdcInsn(result.getIdentifier().getValue());  // 变量名
        mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "getFunctionOrVariable", "(" + Type.STRING + ")" + Type.OBJECT, false);
        return Type.OBJECT;
    }
}
