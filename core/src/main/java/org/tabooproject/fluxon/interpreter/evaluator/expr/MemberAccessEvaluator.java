package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.MemberAccessExpression;
import org.tabooproject.fluxon.runtime.reflection.ReflectionBootstrap;
import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.OBJECT;
import static org.tabooproject.fluxon.runtime.Type.STRING;

/**
 * 成员访问表达式求值器
 * 处理形如 obj.field 或 obj.method(args) 的反射访问表达式
 */
public class MemberAccessEvaluator extends ExpressionEvaluator<MemberAccessExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.MEMBER_ACCESS;
    }

    @Override
    public Object evaluate(Interpreter interpreter, MemberAccessExpression expression) {
        // 求值目标对象
        Object target = interpreter.evaluate(expression.getTarget());
        if (target == null) {
            throw new NullPointerException("Cannot access member '" + expression.getMemberName() + "' on null object");
        }
        try {
            if (expression.isMethodCall()) {
                // 方法调用：obj.method(args)
                Object[] args = new Object[expression.getArgs().length];
                for (int i = 0; i < args.length; i++) {
                    args[i] = interpreter.evaluate(expression.getArgs()[i]);
                }
                return ReflectionHelper.invokeMethod(target, expression.getMemberName(), args);
            } else {
                // 字段访问：obj.field
                return ReflectionHelper.getField(target, expression.getMemberName());
            }
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Failed to access member: " + expression.getMemberName(), e);
        }
    }

    @Override
    public Type generateBytecode(MemberAccessExpression expression, CodeContext ctx, MethodVisitor mv) {
        // 1. 生成 target 的字节码
        Evaluator<ParseResult> targetEval = ctx.getEvaluator(expression.getTarget());
        if (targetEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for target expression");
        }
        Type targetType = targetEval.generateBytecode(expression.getTarget(), ctx, mv);
        // 确保 target 是对象类型
        if (targetType.isPrimitive()) {
            boxing(targetType, mv);
        }
        if (expression.isMethodCall()) {
            // 方法调用：生成 invokedynamic 指令
            // 2. 生成参数的字节码
            int argCount = expression.getArgs().length;
            // 创建参数数组：new Object[argCount]
            mv.visitLdcInsn(argCount);
            mv.visitTypeInsn(ANEWARRAY, OBJECT.getPath());
            for (int i = 0; i < argCount; i++) {
                mv.visitInsn(DUP);  // 复制数组引用
                mv.visitLdcInsn(i); // 数组索引
                // 求值参数
                Evaluator<ParseResult> argEval = ctx.getEvaluator(expression.getArgs()[i]);
                if (argEval == null) {
                    throw new EvaluatorNotFoundError("No evaluator found for argument " + i);
                }
                Type argType = argEval.generateBytecode(expression.getArgs()[i], ctx, mv);
                // 装箱原始类型
                if (argType.isPrimitive()) {
                    boxing(argType, mv);
                }
                mv.visitInsn(AASTORE); // 存储到数组
            }
            // 3. 生成 invokedynamic 指令
            Handle bootstrap = new Handle(
                    H_INVOKESTATIC,
                    ReflectionBootstrap.TYPE.getPath(),
                    "bootstrap",
                    "(" + LOOKUP + STRING + METHOD_TYPE + ")" + CALL_SITE,
                    false
            );
            // 方法描述符：(Object, Object[])Object
            mv.visitInvokeDynamicInsn(expression.getMemberName(), "(" + OBJECT + "[" + OBJECT + ")" + OBJECT, bootstrap);
        } else {
            // 字段访问：生成 invokedynamic 指令
            Handle bootstrapField = new Handle(
                    H_INVOKESTATIC,
                    ReflectionBootstrap.TYPE.getPath(),
                    "bootstrapField",
                    "(" + LOOKUP + STRING + METHOD_TYPE + ")" + CALL_SITE,
                    false
            );
            // 方法描述符：(Object)Object
            mv.visitInvokeDynamicInsn(expression.getMemberName(), "(" + OBJECT + ")" + OBJECT, bootstrapField);
        }
        return OBJECT;
    }

    private static final Type LOOKUP = new Type(MethodHandles.Lookup.class);
    private static final Type METHOD_TYPE = new Type(MethodType.class);
    private static final Type CALL_SITE = new Type(CallSite.class);
}
