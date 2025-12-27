package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.*;

public class FunctionCallEvaluator extends ExpressionEvaluator<FunctionCallExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.FUNCTION_CALL;
    }

    @Override
    public Object evaluate(Interpreter interpreter, FunctionCallExpression result) {
        // 评估参数列表
        ParseResult[] expressionArguments = result.getArguments();
        int argumentCount = expressionArguments.length;
        // fast-args 路径：当参数数量在阈值内时，避免创建参数数组
        if (argumentCount <= 4) {
            return evaluateFastArgs(interpreter, result, expressionArguments, argumentCount);
        }
        // 标准路径：创建参数数组
        Object[] arguments;
        arguments = new Object[argumentCount];
        for (int i = 0; i < argumentCount; i++) {
            arguments[i] = interpreter.evaluate(expressionArguments[i]);
        }
        return Intrinsics.callFunction(
                interpreter.getEnvironment(),
                result.getFunctionName(),
                arguments,
                result.getPositionIndex(),
                result.getExtensionPositionIndex()
        );
    }

    /**
     * fast-args 路径：避免创建参数数组
     */
    private Object evaluateFastArgs(Interpreter interpreter, FunctionCallExpression result, ParseResult[] expressionArguments, int argumentCount) {
        Object arg0 = null, arg1 = null, arg2 = null, arg3 = null;
        // 按从左到右的顺序求值参数，保持与标准路径一致
        if (argumentCount >= 1) {
            arg0 = interpreter.evaluate(expressionArguments[0]);
        }
        if (argumentCount >= 2) {
            arg1 = interpreter.evaluate(expressionArguments[1]);
        }
        if (argumentCount >= 3) {
            arg2 = interpreter.evaluate(expressionArguments[2]);
        }
        if (argumentCount >= 4) {
            arg3 = interpreter.evaluate(expressionArguments[3]);
        }
        return Intrinsics.callFunctionFastArgs(
                interpreter.getEnvironment(),
                result.getFunctionName(),
                argumentCount,
                arg0, arg1, arg2, arg3,
                result.getPositionIndex(),
                result.getExtensionPositionIndex()
        );
    }

    @Override
    public Type generateBytecode(FunctionCallExpression result, CodeContext ctx, MethodVisitor mv) {
        ParseResult[] arguments = result.getArguments();
        int argumentCount = arguments.length;
        // fast-args 路径：参数数量 ≤4 时避免创建数组
        if (argumentCount <= 4) {
            return generateBytecodeFastArgs(result, ctx, mv, arguments, argumentCount);
        } else {
            return generateBytecodeStandard(result, ctx, mv, arguments, argumentCount);
        }
    }

    /**
     * fast-args 字节码生成：避免创建参数数组
     */
    private Type generateBytecodeFastArgs(FunctionCallExpression result, CodeContext ctx, MethodVisitor mv, ParseResult[] arguments, int argumentCount) {
        // 获取环境
        mv.visitVarInsn(ALOAD, 0); // this (RuntimeScriptBase)
        mv.visitFieldInsn(GETFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());
        // 压入函数名
        mv.visitLdcInsn(result.getFunctionName());
        // 压入参数数量
        mv.visitLdcInsn(argumentCount);
        // 按顺序求值并压入 arg0..arg3，不足的压入 null
        for (int i = 0; i < 4; i++) {
            if (i < argumentCount) {
                generateArgumentBytecode(ctx, mv, arguments[i]);
            } else {
                mv.visitInsn(ACONST_NULL);
            }
        }
        // 压入位置参数
        mv.visitLdcInsn(result.getPositionIndex());
        mv.visitLdcInsn(result.getExtensionPositionIndex());
        // 调用 Intrinsics.callFunctionFastArgs 方法
        // 签名: (Environment, String, int, Object, Object, Object, Object, int, int) -> Object
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "callFunctionFastArgs",
                "(" + Environment.TYPE + Type.STRING + Type.I + Type.OBJECT + Type.OBJECT + Type.OBJECT + Type.OBJECT + Type.I + Type.I + ")" + Type.OBJECT,
                false
        );
        return Type.OBJECT;
    }

    /**
     * 标准路径字节码生成：创建参数数组
     */
    private Type generateBytecodeStandard(FunctionCallExpression result, CodeContext ctx, MethodVisitor mv, ParseResult[] arguments, int argumentCount) {
        // 标准路径：创建参数数组
        // 获取环境
        mv.visitVarInsn(ALOAD, 0); // this (RuntimeScriptBase)
        mv.visitFieldInsn(GETFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());
        // 压入字符串
        mv.visitLdcInsn(result.getFunctionName());
        // 创建参数数组
        mv.visitLdcInsn(argumentCount);
        mv.visitTypeInsn(ANEWARRAY, Type.OBJECT.getPath());
        // 填充参数数组
        for (int i = 0; i < argumentCount; i++) {
            mv.visitInsn(DUP);  // 复制数组引用
            mv.visitLdcInsn(i); // 数组索引
            generateArgumentBytecode(ctx, mv, arguments[i]);
            mv.visitInsn(AASTORE); // 存储到数组
        }
        // 压入位置参数
        mv.visitLdcInsn(result.getPositionIndex());
        mv.visitLdcInsn(result.getExtensionPositionIndex());
        // 调用 Intrinsics.callFunction 方法
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "callFunction",
                "(" + Environment.TYPE + Type.STRING + OBJECT_ARRAY + Type.I + Type.I + ")" + Type.OBJECT,
                false
        );
        return Type.OBJECT;
    }

    /**
     * 生成单个参数表达式的字节码
     */
    private void generateArgumentBytecode(CodeContext ctx, MethodVisitor mv, ParseResult argument) {
        Evaluator<ParseResult> argEval = ctx.getEvaluator(argument);
        if (argEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for argument expression");
        }
        if (argEval.generateBytecode(argument, ctx, mv) == Type.VOID) {
            throw new VoidError("Void type is not allowed for function arguments");
        }
    }

    private static final Type OBJECT_ARRAY = new Type(Object.class, 1);
}
