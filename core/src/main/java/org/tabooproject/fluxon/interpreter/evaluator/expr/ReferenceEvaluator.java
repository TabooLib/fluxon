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
        int position = result.getPosition();
        Environment env = interpreter.getEnvironment();
        if (position >= 0) {
            // 使用当前环境的类型（作用域隔离）
            Type varType = env.getVariableType(position);
            if (varType.isPrimitive()) {
                if (varType == Type.I || varType == Type.Z) {
                    interpreter.resultPrimitive = env.getLocalInt(position);
                } else if (varType == Type.J) {
                    interpreter.resultPrimitive = env.getLocalLong(position);
                } else if (varType == Type.F) {
                    interpreter.resultPrimitive = Float.floatToRawIntBits(env.getLocalFloat(position));
                } else {
                    interpreter.resultPrimitive = Double.doubleToRawLongBits(env.getLocalDouble(position));
                }
                return varType;
            }
            interpreter.resultRef = env.getLocalRef(position);
            return varType;
        }
        // root 变量
        String name = result.getIdentifier().getValue();
        Object value = Intrinsics.getVariableOrFunction(env, name, result.isOptional(), position);
        Type rootType = interpreter.getRootVariableType(name);
        if (rootType.isPrimitive() && value != null) {
            interpreter.resultPrimitive = Type.unbox(value, rootType);
            return rootType;
        }
        interpreter.resultRef = value;
        return rootType;
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
                return varType;
            }
        }
        // root 变量：先获取 Object，再根据类型拆箱
        String name = result.getIdentifier().getValue();
        mv.visitLdcInsn(name);
        mv.visitInsn(result.isOptional() ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        mv.visitLdcInsn(-1);
        mv.visitMethodInsn(INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "getVariableOrFunction",
                "(" + Environment.TYPE + Type.STRING + Type.Z + Type.I + ")" + Type.OBJECT,
                false
        );
        Type rootType = ctx.getRootVariableType(name);
        if (rootType.isPrimitive()) {
            Instructions.unbox(mv, rootType);
        }
        return rootType;
    }

    @Override
    public Type inferResultType(ReferenceExpression result, TypeAnalyzer analyzer) {
        int position = result.getPosition();
        if (position >= 0) {
            return analyzer.getVariableType(position);
        } else {
            return analyzer.getRootVariableType(result.getIdentifier().getValue());
        }
    }

    /**
     * 根据类型调用对应的 getter 方法
     * 栈输入：[env, index]
     * 栈输出：[value]
     */
    public static void emitGetLocal(Type type, MethodVisitor mv) {
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
    public static void emitSetLocal(Type type, MethodVisitor mv) {
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
