package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FunctionContextPool;
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
    public Type evaluate(Interpreter interpreter, FunctionCallExpression result) {
        ParseResult[] expressionArguments = result.getArguments();
        int argumentCount = expressionArguments.length;
        Object[] arguments = new Object[argumentCount];
        for (int i = 0; i < argumentCount; i++) {
            Type t = interpreter.evaluate(expressionArguments[i]);
            arguments[i] = interpreter.getResultBoxed(t);
        }
        interpreter.resultRef = Intrinsics.callFunction(
                FunctionContextPool.local(),
                interpreter.getEnvironment(),
                result.getFunctionName(),
                arguments,
                result.getPositionIndex(),
                result.getExtensionPositionIndex()
        );
        return Type.OBJECT;
    }

    @Override
    public Type generateBytecode(FunctionCallExpression result, CodeContext ctx, MethodVisitor mv) {
        ParseResult[] arguments = result.getArguments();
        int argumentCount = arguments.length;
        // 加载 pool
        Instructions.loadPool(mv, ctx);
        // 加载环境
        Instructions.loadEnvironment(mv, ctx);
        // 压入函数名
        mv.visitLdcInsn(result.getFunctionName());
        // 创建参数数组
        mv.visitLdcInsn(argumentCount);
        mv.visitTypeInsn(ANEWARRAY, Type.OBJECT.getPath());
        // 填充参数数组
        for (int i = 0; i < argumentCount; i++) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            generateArgumentBytecode(ctx, mv, arguments[i]);
            mv.visitInsn(AASTORE);
        }
        // 压入位置参数
        mv.visitLdcInsn(result.getPositionIndex());
        mv.visitLdcInsn(result.getExtensionPositionIndex());
        // 调用 Intrinsics.callFunction
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "callFunction",
                "(" + FunctionContextPool.TYPE + Environment.TYPE + Type.STRING + OBJECT_ARRAY + Type.I + Type.I + ")" + Type.OBJECT,
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
