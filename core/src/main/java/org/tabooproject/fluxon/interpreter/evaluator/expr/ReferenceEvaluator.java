package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.error.VariableNotFoundException;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.VariablePosition;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
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
        // 获取变量位置
        VariablePosition pos = result.getPosition();
        int level = pos != null ? pos.getLevel() : 0;
        int index = pos != null ? pos.getIndex() : 0;
        // 获取变量值
        Object var = environment.getOrNull(result.getIdentifier().getValue(), level, index);
        if (var != null || result.isOptional()) {
            return var;
        }
        // 获取函数
        Function fun = environment.getFunctionOrNull(result.getIdentifier().getValue());
        if (fun != null) {
            return fun;
        }
        throw new VariableNotFoundException(result.getIdentifier().getValue());
    }

    @Override
    public Type generateBytecode(ReferenceExpression result, CodeContext ctx, MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);                   // this
        mv.visitLdcInsn(result.getIdentifier().getValue());  // 变量名
        mv.visitInsn(result.isOptional() ? Opcodes.ICONST_1 : Opcodes.ICONST_0);  // isOptional
        // 压入 level 和 index 参数
        VariablePosition pos = result.getPosition();
        int level = pos != null ? pos.getLevel() : 0;
        int index = pos != null ? pos.getIndex() : 0;
        mv.visitIntInsn(Opcodes.BIPUSH, level);
        mv.visitIntInsn(Opcodes.BIPUSH, index);
        mv.visitMethodInsn(INVOKEVIRTUAL, ctx.getClassName(), "getVariableOrFunction", "(" + Type.STRING + Type.Z + Type.I + Type.I + ")" + Type.OBJECT, false);
        return Type.OBJECT;
    }
}
