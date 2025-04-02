package org.tabooproject.fluxon.interpreter.evaluator;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public abstract class Evaluator<T extends ParseResult> {

    /**
     * 评估结果
     */
    abstract public Object evaluate(Interpreter interpreter, T result);

    /**
     * 生成字节码
     *
     * @param result 解析结果
     * @param ctx    代码上下文
     * @param mv     方法访问器
     */
    abstract public Type generateBytecode(T result, CodeContext ctx, MethodVisitor mv);

    /**
     * 将操作数装箱
     *
     * @param type 类型
     * @return 装箱后的类型
     */
    protected Type boxing(Type type, MethodVisitor mv) {
        switch (type.getDescriptor()) {
            case "I":
                mv.visitMethodInsn(INVOKESTATIC, Type.INT.getPath(), "valueOf", "(I)" + Type.INT, false);
                return Type.INT;
            case "J":
                mv.visitMethodInsn(INVOKESTATIC, Type.LONG.getPath(), "valueOf", "(J)" + Type.LONG, false);
                return Type.LONG;
            case "F":
                mv.visitMethodInsn(INVOKESTATIC, Type.FLOAT.getPath(), "valueOf", "(F)" + Type.FLOAT, false);
                return Type.FLOAT;
            case "D":
                mv.visitMethodInsn(INVOKESTATIC, Type.DOUBLE.getPath(), "valueOf", "(D)" + Type.DOUBLE, false);
                return Type.DOUBLE;
            case "Z":
                mv.visitMethodInsn(INVOKESTATIC, Type.BOOLEAN.getPath(), "valueOf", "(Z)" + Type.BOOLEAN, false);
                return Type.BOOLEAN;
        }
        return type;
    }
}
