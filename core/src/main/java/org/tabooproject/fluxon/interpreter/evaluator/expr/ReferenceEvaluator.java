package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.error.VariableNotFoundException;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;

public class ReferenceEvaluator extends ExpressionEvaluator<ReferenceExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.REFERENCE;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ReferenceExpression result) {
        Environment environment = interpreter.getEnvironment();
        // 获取变量值
        Object var = environment.getOrNull(result.getIdentifier().getValue(), result.getPosition());
        if (var != null) {
            return var;
        }
        // 获取函数
        Function fun = environment.getFunctionOrNull(result.getIdentifier().getValue());
        if (fun != null) {
            return fun;
        }
        if (result.isOptional()) {
            return null;
        }
        throw new VariableNotFoundException(result.getIdentifier().getValue());
    }

    @Override
    public Type generateBytecode(ReferenceExpression result, CodeContext ctx, MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 0);                           // this
        mv.visitLdcInsn(result.getIdentifier().getValue());  // 变量名
        mv.visitInsn(result.isOptional() ? Opcodes.ICONST_1 : Opcodes.ICONST_0);  // isOptional
        // 压入 index 参数
        mv.visitLdcInsn(result.getPosition());
        mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "getVariableOrFunction", "(" + Type.STRING + Type.Z + Type.I + ")" + Type.OBJECT, false);
        return Type.OBJECT;
    }
}
