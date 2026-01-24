package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
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
            Type varType = ctx.getVariableType(position);
            mv.visitLdcInsn(position);
            if (varType.isPrimitive()) {
                emitGetLocal(varType, mv);
                return varType;
            } else {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        Environment.TYPE.getPath(),
                        "getLocalRef",
                        "(" + Type.I + ")" + Type.OBJECT,
                        false
                );
                return Type.OBJECT;
            }
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

    @Override
    public Type inferResultType(ReferenceExpression result, TypeAnalyzer analyzer) {
        int position = result.getPosition();
        if (position >= 0) {
            return analyzer.getVariableType(position);
        }
        return Type.OBJECT;
    }

    /**
     * 根据类型调用对应的 getter 方法
     * 栈输入：[env, index]
     * 栈输出：[value]
     */
    static void emitGetLocal(Type type, MethodVisitor mv) {
        String name;
        String desc;
        if (type == Type.I || type == Type.Z) {
            name = "getLocalInt";
            desc = "(" + Type.I + ")" + Type.I;
        } else if (type == Type.J) {
            name = "getLocalLong";
            desc = "(" + Type.I + ")" + Type.J;
        } else if (type == Type.F) {
            name = "getLocalFloat";
            desc = "(" + Type.I + ")" + Type.F;
        } else {
            name = "getLocalDouble";
            desc = "(" + Type.I + ")" + Type.D;
        }
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Environment.TYPE.getPath(), name, desc, false);
    }

    /**
     * 根据类型调用对应的 setter 方法
     * 栈输入：[env, index, value]
     * 栈输出：[]
     */
    static void emitSetLocal(Type type, MethodVisitor mv) {
        String name;
        String desc;
        if (type == Type.I || type == Type.Z) {
            name = "setLocalInt";
            desc = "(" + Type.I + Type.I + ")" + Type.VOID;
        } else if (type == Type.J) {
            name = "setLocalLong";
            desc = "(" + Type.I + Type.J + ")" + Type.VOID;
        } else if (type == Type.F) {
            name = "setLocalFloat";
            desc = "(" + Type.I + Type.F + ")" + Type.VOID;
        } else {
            name = "setLocalDouble";
            desc = "(" + Type.I + Type.D + ")" + Type.VOID;
        }
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Environment.TYPE.getPath(), name, desc, false);
    }
}
