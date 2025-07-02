package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.FunctionCall;

import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class FunctionCallEvaluator extends ExpressionEvaluator<FunctionCall> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.FUNCTION_CALL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, FunctionCall result) {
        // 评估被调用者
        Object callee = interpreter.evaluate(result.getCallee());
        // 评估参数列表
        Object[] arguments = new Object[result.getArguments().size()];
        List<ParseResult> expressionArguments = result.getArguments();
        for (int i = 0; i < expressionArguments.size(); i++) {
            ParseResult argument = expressionArguments.get(i);
            arguments[i] = interpreter.evaluate(argument);
        }
        // 使用 Operations.callFunction 执行函数调用
        return Intrinsics.callFunction(interpreter.getEnvironment(), callee, arguments);
    }

    @Override
    public Type generateBytecode(FunctionCall result, CodeContext ctx, MethodVisitor mv) {
        // 获取被调用者的评估器
        Evaluator<ParseResult> calleeEval = ctx.getEvaluator(result.getCallee());
        if (calleeEval == null) {
            throw new RuntimeException("No evaluator found for callee expression");
        }
        // 获取环境
        mv.visitVarInsn(ALOAD, 0); // this (RuntimeScriptBase)
        mv.visitFieldInsn(GETFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());
        // 评估被调用者
        calleeEval.generateBytecode(result.getCallee(), ctx, mv);

        // 创建参数数组
        List<ParseResult> arguments = result.getArguments();
        mv.visitLdcInsn(arguments.size());
        mv.visitTypeInsn(ANEWARRAY, Type.OBJECT.getPath());

        // 填充参数数组
        for (int i = 0; i < arguments.size(); i++) {
            mv.visitInsn(DUP);  // 复制数组引用
            mv.visitLdcInsn(i); // 数组索引
            // 评估参数表达式
            Evaluator<ParseResult> argEval = ctx.getEvaluator(arguments.get(i));
            if (argEval == null) {
                throw new RuntimeException("No evaluator found for argument expression");
            }
            if (argEval.generateBytecode(arguments.get(i), ctx, mv) == Type.VOID) {
                throw new RuntimeException("Void type is not allowed for function arguments");
            }
            mv.visitInsn(AASTORE); // 存储到数组
        }

        // 调用 Operations.callFunction 方法
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "callFunction",
                "(" + Environment.TYPE + Type.OBJECT + OBJECT_ARRAY + ")" + Type.OBJECT,
                false
        );
        return Type.OBJECT;
    }

    private static final Type OBJECT_ARRAY = new Type(Object.class, 1);
}
