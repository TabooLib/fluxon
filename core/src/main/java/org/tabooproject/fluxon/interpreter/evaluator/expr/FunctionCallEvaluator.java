package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.Expression;
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
        // 评估被调用者
        // 评估参数列表
        ParseResult[] expressionArguments = result.getArguments();
        int argumentCount = expressionArguments.length;
        Object[] arguments;
        if (argumentCount == 0) {
            arguments = EMPTY_ARGUMENTS;
        } else {
            arguments = new Object[argumentCount];
            for (int i = 0; i < argumentCount; i++) {
                ParseResult argument = expressionArguments[i];
                if (argument.getType() == ParseResult.ResultType.EXPRESSION) {
                    arguments[i] = interpreter.evaluateExpression((Expression) argument);
                } else {
                    arguments[i] = interpreter.evaluate(argument);
                }
            }
        }
        Environment environment = interpreter.getEnvironment();
        int pos1 = result.getPosition() != null ? result.getPosition().getIndex() : -1;
        int pos2 = result.getExtensionPosition() != null ? result.getExtensionPosition().getIndex() : -1;
        return Intrinsics.callFunction(environment, result.getCallee(), arguments, pos1, pos2);
    }

    @Override
    public Type generateBytecode(FunctionCallExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取环境
        mv.visitVarInsn(ALOAD, 0); // this (RuntimeScriptBase)
        mv.visitFieldInsn(GETFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());
        // 压入字符串
        mv.visitLdcInsn(result.getCallee());

        // 创建参数数组
        ParseResult[] arguments = result.getArguments();
        mv.visitLdcInsn(arguments.length);
        mv.visitTypeInsn(ANEWARRAY, Type.OBJECT.getPath());

        // 填充参数数组
        for (int i = 0; i < arguments.length; i++) {
            mv.visitInsn(DUP);  // 复制数组引用
            mv.visitLdcInsn(i); // 数组索引
            // 评估参数表达式
            Evaluator<ParseResult> argEval = ctx.getEvaluator(arguments[i]);
            if (argEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for argument expression");
            }
            if (argEval.generateBytecode(arguments[i], ctx, mv) == Type.VOID) {
                throw new VoidError("Void type is not allowed for function arguments");
            }
            mv.visitInsn(AASTORE); // 存储到数组
        }

        // 压入位置参数
        int pos1 = result.getPosition() != null ? result.getPosition().getIndex() : -1;
        int pos2 = result.getExtensionPosition() != null ? result.getExtensionPosition().getIndex() : -1;
        mv.visitLdcInsn(pos1);
        mv.visitLdcInsn(pos2);

        // 调用 Operations.callFunction 方法
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "callFunction",
                "(" + Environment.TYPE + Type.STRING + OBJECT_ARRAY + Type.I + Type.I + ")" + Type.OBJECT,
                false
        );
        return Type.OBJECT;
    }

    private static final Type OBJECT_ARRAY = new Type(Object.class, 1);
    private static final Object[] EMPTY_ARGUMENTS = new Object[0];
}
