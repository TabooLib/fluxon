package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.NewExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.reflection.ReflectionBootstrap;
import org.tabooproject.fluxon.runtime.reflection.ReflectionHelper;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.OBJECT;
import static org.tabooproject.fluxon.runtime.Type.STRING;

/**
 * new 表达式求值器
 * 处理形如 new java.util.ArrayList() 的对象构造表达式
 */
public class NewEvaluator extends ExpressionEvaluator<NewExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.NEW;
    }

    @Override
    public Object evaluate(Interpreter interpreter, NewExpression expression) {
        String className = expression.getClassName();
        // 加载类
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
        // 评估参数
        ParseResult[] argExpressions = expression.getArguments();
        Object[] args = new Object[argExpressions.length];
        for (int i = 0; i < argExpressions.length; i++) {
            args[i] = interpreter.evaluate(argExpressions[i]);
        }
        // 调用构造函数
        try {
            return ReflectionHelper.invokeConstructor(clazz, args);
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Failed to construct object of class: " + className, e);
        }
    }

    @Override
    public Type generateBytecode(NewExpression expression, CodeContext ctx, MethodVisitor mv) {
        // 1. 加载类名
        mv.visitLdcInsn(expression.getClassName());
        // 2. 生成参数数组
        ParseResult[] arguments = expression.getArguments();
        int argCount = arguments.length;
        mv.visitLdcInsn(argCount);
        mv.visitTypeInsn(ANEWARRAY, OBJECT.getPath());
        for (int i = 0; i < argCount; i++) {
            mv.visitInsn(DUP);  // 复制数组引用
            mv.visitLdcInsn(i); // 数组索引
            // 求值参数
            Evaluator<ParseResult> argEval = ctx.getEvaluator(arguments[i]);
            if (argEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for argument " + i);
            }
            Type argType = argEval.generateBytecode(arguments[i], ctx, mv);
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
                "bootstrapConstructor",
                "(" + LOOKUP + STRING + METHOD_TYPE + ")" + CALL_SITE,
                false
        );
        // 方法描述符：(String, Object[])Object
        mv.visitInvokeDynamicInsn("construct", "(" + STRING + "[" + OBJECT + ")" + OBJECT, bootstrap);
        return OBJECT;
    }

    private static final Type LOOKUP = new Type(java.lang.invoke.MethodHandles.Lookup.class);
    private static final Type METHOD_TYPE = new Type(java.lang.invoke.MethodType.class);
    private static final Type CALL_SITE = new Type(java.lang.invoke.CallSite.class);
}
