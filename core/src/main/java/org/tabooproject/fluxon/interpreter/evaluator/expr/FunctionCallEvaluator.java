package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
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
        FunctionContext<?> ctx = Intrinsics.prepareCall(
                FunctionContextPool.local(),
                interpreter.getEnvironment(),
                result.getFunctionName(),
                argumentCount,
                result.getPositionIndex(),
                result.getExtensionPositionIndex()
        );
        for (int i = 0; i < argumentCount; i++) {
            Type t = interpreter.evaluate(expressionArguments[i]);
            if (t.isPrimitive()) {
                switch (t.getDescriptor()) {
                    case "I":
                    case "Z":
                        ctx.setInt(i, (int) interpreter.resultPrimitive);
                        break;
                    case "J":
                        ctx.setLong(i, interpreter.resultPrimitive);
                        break;
                    case "F":
                        ctx.setFloat(i, Float.intBitsToFloat((int) interpreter.resultPrimitive));
                        break;
                    case "D":
                        ctx.setDouble(i, Double.longBitsToDouble(interpreter.resultPrimitive));
                        break;
                }
            } else {
                ctx.setRef(i, interpreter.resultRef);
            }
        }
        interpreter.resultRef = Intrinsics.finishCall(ctx, interpreter);
        return Type.OBJECT;
    }

    @Override
    public Type generateBytecode(FunctionCallExpression result, CodeContext ctx, MethodVisitor mv) {
        ParseResult[] arguments = result.getArguments();
        int argumentCount = arguments.length;
        int savedLocalVar = ctx.getLocalVarIndex();
        // 1. 调用 prepareCall → FunctionContext
        Instructions.loadPool(mv, ctx);
        Instructions.loadEnvironment(mv, ctx);
        mv.visitLdcInsn(result.getFunctionName());
        mv.visitLdcInsn(argumentCount);
        mv.visitLdcInsn(result.getPositionIndex());
        mv.visitLdcInsn(result.getExtensionPositionIndex());
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "prepareCall",
                "(" + FunctionContextPool.TYPE + Environment.TYPE + Type.STRING + "III)" + FunctionContext.TYPE,
                false
        );
        // 2. 存入局部变量
        int ctxSlot = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, ctxSlot);
        // 3. 逐个设置参数
        for (int i = 0; i < argumentCount; i++) {
            mv.visitVarInsn(ALOAD, ctxSlot);
            mv.visitLdcInsn(i);
            Evaluator<ParseResult> argEval = ctx.getEvaluator(arguments[i]);
            if (argEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for argument expression");
            }
            Type t = argEval.generateBytecode(arguments[i], ctx, mv);
            if (t == Type.VOID) {
                throw new VoidError("Void type is not allowed for function arguments");
            }
            emitSetArg(t, mv);
        }
        // 4. 调用 finishCall
        mv.visitVarInsn(ALOAD, ctxSlot);
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "finishCall",
                "(" + FunctionContext.TYPE + ")" + Type.OBJECT,
                false
        );
        // 释放临时变量槽位
        ctx.restoreLocalVarIndex(savedLocalVar);
        return Type.OBJECT;
    }

    /**
     * 根据参数类型生成对应的 FunctionContext setter 调用
     */
    private static void emitSetArg(Type t, MethodVisitor mv) {
        String ctxPath = FunctionContext.TYPE.getPath();
        if (t == Type.I || t == Type.Z) {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setInt", "(II)V", false);
        } else if (t == Type.J) {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setLong", "(IJ)V", false);
        } else if (t == Type.F) {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setFloat", "(IF)V", false);
        } else if (t == Type.D) {
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setDouble", "(ID)V", false);
        } else {
            // Object 类型
            mv.visitMethodInsn(INVOKEVIRTUAL, ctxPath, "setRef", "(I" + Type.OBJECT + ")V", false);
        }
    }

    @Override
    public void analyzeTypes(FunctionCallExpression result, TypeAnalyzer analyzer) {
        for (ParseResult arg : result.getArguments()) {
            analyzer.analyzeNode(arg);
        }
    }

    @Override
    public Type inferResultType(FunctionCallExpression result, TypeAnalyzer analyzer) {
        // 尝试通过函数名从全局运行时查找函数
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        Function function = runtime.getSystemFunctions().get(result.getFunctionName());
        if (function != null) {
            return function.getReturnType();
        }
        return Type.OBJECT;
    }
}
