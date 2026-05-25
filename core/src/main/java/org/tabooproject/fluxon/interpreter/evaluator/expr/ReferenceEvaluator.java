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
import org.tabooproject.fluxon.runtime.FunctionContext;
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
        // Env-free 路径：从 FunctionContext 数组读取
        FunctionContext<?> ctx = interpreter.activeFunctionContext;
        if (position >= 0 && ctx != null) {
            interpreter.resultRef = ctx.getLocal(position);
            return Type.OBJECT;
        }
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
        Object value = Intrinsics.getVariable(env, name, result.isOptional(), position);
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
        if (position >= 0) {
            CodeContext.InlineLocalVariable inlineLocal = ctx.getInlineLocalVariable(position);
            if (inlineLocal != null) {
                // 内联函数体读取参数临时槽位，避免退回 FunctionContext 或 Environment。
                Instructions.emitLoadLocal(mv, inlineLocal.type, inlineLocal.slot);
                return inlineLocal.type;
            }
            // Env-free 模式：从 JVM 局部变量读取
            if (ctx.isEnvFreeMode()) {
                int jvmSlot = ctx.getJvmSlot(position);
                Type varType = ctx.getVariableType(position);
                Instructions.emitLoadLocal(mv, varType, jvmSlot);
                return varType;
            }
            Instructions.loadEnvironment(mv, ctx);
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
                emitReferenceCast(varType, mv);
                return varType;
            }
        }
        // root 变量：先获取 Object，再根据类型拆箱
        String name = result.getIdentifier().getValue();
        CodeContext.RootVariableCache cache = ctx.getRootVariableCache(name);
        if (cache != null) {
            // 循环内已证明不可外部观察的 root 读，直接读取缓存槽位。
            Instructions.emitLoadLocal(mv, cache.type, cache.slot);
            return cache.type;
        }
        Type rootType = ctx.getRootVariableType(name);
        if (ctx.isRootConstantReferenceMode() && emitRootConstantReference(ctx.getRootConstantValue(name), rootType, mv)) {
            // 纯直线表达式内的顶层常量读取不需要回到 Environment map。
            return rootType;
        }
        Instructions.loadEnvironment(mv, ctx);
        mv.visitLdcInsn(name);
        mv.visitInsn(result.isOptional() ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        mv.visitLdcInsn(-1);
        mv.visitMethodInsn(INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "getVariable",
                "(" + Environment.TYPE + Type.STRING + Type.Z + Type.I + ")" + Type.OBJECT,
                false
        );
        if (rootType.isPrimitive()) {
            Instructions.unbox(mv, rootType);
        } else {
            emitReferenceCast(rootType, mv);
        }
        return rootType;
    }

    private static boolean emitRootConstantReference(Object value, Type type, MethodVisitor mv) {
        if (!(value instanceof Number)) return false;
        Number number = (Number) value;
        if (type == Type.I) {
            mv.visitLdcInsn(number.intValue());
        } else if (type == Type.J) {
            mv.visitLdcInsn(number.longValue());
        } else if (type == Type.F) {
            mv.visitLdcInsn(number.floatValue());
        } else if (type == Type.D) {
            mv.visitLdcInsn(number.doubleValue());
        } else {
            return false;
        }
        return true;
    }

    private static void emitReferenceCast(Type type, MethodVisitor mv) {
        if (type != Type.STRING) return;
        // 仅收窄字符串引用，集合和解构槽位存在复用，不能按推断容器类型强制 cast。
        mv.visitTypeInsn(Opcodes.CHECKCAST, type.getPath());
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
