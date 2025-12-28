package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.StaticAccessExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.reflection.ReflectionBootstrap;
import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.OBJECT;
import static org.tabooproject.fluxon.runtime.Type.STRING;

/**
 * 静态成员访问表达式求值器
 * 处理形如 static com.example.MyClass.method(args) 或 static com.example.MyClass.field 的表达式
 */
public class StaticAccessEvaluator extends ExpressionEvaluator<StaticAccessExpression> {

    private static final Type CLASS_TYPE = new Type(Class.class);
    private static final Type LOOKUP = new Type(MethodHandles.Lookup.class);
    private static final Type METHOD_TYPE = new Type(MethodType.class);
    private static final Type CALL_SITE = new Type(CallSite.class);

    @Override
    public ExpressionType getType() {
        return ExpressionType.STATIC_ACCESS;
    }

    @Override
    public Object evaluate(Interpreter interpreter, StaticAccessExpression expression) {
        // 加载类
        Class<?> clazz;
        try {
            clazz = Class.forName(expression.getClassName(), true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + expression.getClassName(), e);
        }
        try {
            if (expression.isMethodCall()) {
                // 静态方法调用
                Object[] args = new Object[expression.getArguments().length];
                for (int i = 0; i < args.length; i++) {
                    args[i] = interpreter.evaluate(expression.getArguments()[i]);
                }
                return ReflectionHelper.invokeStaticMethod(clazz, expression.getMemberName(), args);
            } else {
                // 静态字段访问
                return ReflectionHelper.getStaticField(clazz, expression.getMemberName());
            }
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Failed to access static member: " + expression.getMemberName(), e);
        }
    }

    @Override
    public Type generateBytecode(StaticAccessExpression expression, CodeContext ctx, MethodVisitor mv) {
        // 1. 加载类对象
        mv.visitLdcInsn(org.objectweb.asm.Type.getType("L" + expression.getClassName().replace('.', '/') + ";"));
        // 如果是方法调用
        if (expression.isMethodCall()) {
            // 静态方法调用：生成参数数组
            int argCount = expression.getArguments().length;
            mv.visitLdcInsn(argCount);
            mv.visitTypeInsn(ANEWARRAY, OBJECT.getPath());
            for (int i = 0; i < argCount; i++) {
                mv.visitInsn(DUP);
                mv.visitLdcInsn(i);
                Evaluator<ParseResult> argEval = ctx.getEvaluator(expression.getArguments()[i]);
                if (argEval == null) {
                    throw new EvaluatorNotFoundError("No evaluator found for argument " + i);
                }
                Type argType = argEval.generateBytecode(expression.getArguments()[i], ctx, mv);
                if (argType.isPrimitive()) {
                    boxing(argType, mv);
                }
                mv.visitInsn(AASTORE);
            }
            // 生成 invokedynamic 指令调用静态方法
            Handle bootstrap = new Handle(
                    H_INVOKESTATIC,
                    ReflectionBootstrap.TYPE.getPath(),
                    "bootstrapStaticMethod",
                    "(" + LOOKUP + STRING + METHOD_TYPE + ")" + CALL_SITE,
                    false
            );
            mv.visitInvokeDynamicInsn(expression.getMemberName(), "(" + CLASS_TYPE + "[" + OBJECT + ")" + OBJECT, bootstrap);
        } else {
            // 静态字段访问：生成 invokedynamic 指令
            Handle bootstrapField = new Handle(
                    H_INVOKESTATIC,
                    ReflectionBootstrap.TYPE.getPath(),
                    "bootstrapStaticField",
                    "(" + LOOKUP + STRING + METHOD_TYPE + ")" + CALL_SITE,
                    false
            );
            mv.visitInvokeDynamicInsn(expression.getMemberName(), "(" + CLASS_TYPE + ")" + OBJECT, bootstrapField);
        }
        return OBJECT;
    }
}
