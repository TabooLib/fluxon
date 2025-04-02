package org.tabooproject.fluxon.interpreter.evaluator;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public abstract class ExpressionEvaluator<T extends Expression> implements Evaluator<T> {

    /**
     * 表达式类型
     */
    abstract public ExpressionType getType();

    @Override
    public Type generateBytecode(T result, CodeContext ctx, MethodVisitor mv) {
        return Type.OBJECT;
    }

    /**
     * 将操作数装箱
     */
    protected void boxing(Type type, MethodVisitor mv) {
        switch (type.getDescriptor()) {
            case "Z":
                mv.visitMethodInsn(INVOKESTATIC, Type.BOOLEAN.getPath(), "valueOf", "(Z)" + Type.BOOLEAN, false);
                break;
            case "I":
                mv.visitMethodInsn(INVOKESTATIC, Type.INT.getPath(), "valueOf", "(I)" + Type.INT, false);
                break;
            case "J":
                mv.visitMethodInsn(INVOKESTATIC, Type.LONG.getPath(), "valueOf", "(J)" + Type.LONG, false);
                break;
            case "F":
                mv.visitMethodInsn(INVOKESTATIC, Type.FLOAT.getPath(), "valueOf", "(F)" + Type.FLOAT, false);
                break;
            case "D":
                mv.visitMethodInsn(INVOKESTATIC, Type.DOUBLE.getPath(), "valueOf", "(D)" + Type.DOUBLE, false);
                break;
        }
    }
}
