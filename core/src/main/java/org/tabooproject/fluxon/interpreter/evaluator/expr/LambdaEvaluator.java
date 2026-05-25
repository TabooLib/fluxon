package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.LambdaExpression;
import org.tabooproject.fluxon.runtime.CaptureCell;
import org.tabooproject.fluxon.runtime.CaptureFrame;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Type;

import java.util.HashSet;

import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;

/**
 * Lambda 表达式求值/生成
 */
public class LambdaEvaluator extends ExpressionEvaluator<LambdaExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.LAMBDA;
    }

    @Override
    public Type evaluate(Interpreter interpreter, LambdaExpression expr) {
        Function function = interpreter.getOrCreateLambda(expr);
        if (expr.getCaptureOffset() > 0) {
            FunctionContext<?> active = interpreter.activeFunctionContext;
            if (active != null) {
                CaptureFrame frame = active.createCaptureFrame(expr.getCaptureOffset());
                interpreter.resultRef = interpreter.getEnvironment().captureFunction(function, frame);
                return Type.OBJECT;
            }
            interpreter.resultRef = interpreter.getEnvironment().captureFunction(function);
            return Type.OBJECT;
        }
        interpreter.resultRef = function;
        return Type.OBJECT;
    }

    @Override
    public Type generateBytecode(LambdaExpression result, CodeContext ctx, MethodVisitor mv) {
        LambdaFunctionDefinition definition = (LambdaFunctionDefinition) result.toFunctionDefinition(ctx.getClassName());
        ctx.addLambdaDefinition(definition);
        String lambdaClassName = definition.getOwnerClassName() + definition.getName();
        if (definition.getCaptureOffset() > 0) {
            // 捕获型 Lambda 仍绑定当前 Environment，但同一环境内复用包装对象，避免循环求值反复分配。
            Instructions.loadEnvironment(mv, ctx);
            mv.visitFieldInsn(GETSTATIC, ctx.getClassName(), definition.getName(), "L" + lambdaClassName + ";");
            if (ctx.isEnvFreeMode()) {
                emitCaptureFrame(result.getCaptureOffset(), ctx, mv);
                mv.visitMethodInsn(
                        INVOKEVIRTUAL,
                        Environment.TYPE.getPath(),
                        "captureFunction",
                        "(" + Function.TYPE + CaptureFrame.TYPE + ")" + Function.TYPE,
                        false
                );
                return Function.TYPE;
            }
            mv.visitMethodInsn(
                    INVOKEVIRTUAL,
                    Environment.TYPE.getPath(),
                    "captureFunction",
                    "(" + Function.TYPE + ")" + Function.TYPE,
                    false
            );
            return Function.TYPE;
        }
        mv.visitFieldInsn(GETSTATIC, ctx.getClassName(), definition.getName(), "L" + lambdaClassName + ";");
        return Function.TYPE;
    }

    private void emitCaptureFrame(int captureOffset, CodeContext ctx, MethodVisitor mv) {
        mv.visitTypeInsn(NEW, CaptureFrame.TYPE.getPath());
        mv.visitInsn(DUP);
        mv.visitLdcInsn(captureOffset);
        mv.visitMethodInsn(INVOKESPECIAL, CaptureFrame.TYPE.getPath(), "<init>", "(" + Type.I + ")V", false);
        int frameSlot = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, frameSlot);
        for (int i = 0; i < captureOffset; i++) {
            if (!ctx.isLocalCapturedByChild(i)) continue;
            mv.visitVarInsn(ALOAD, frameSlot);
            mv.visitLdcInsn(i);
            mv.visitVarInsn(ALOAD, ctx.hasCaptureCellSlot(i) ? ctx.getCaptureCellSlot(i) : ctx.getJvmSlot(i));
            mv.visitTypeInsn(CHECKCAST, CaptureCell.TYPE.getPath());
            mv.visitMethodInsn(INVOKEVIRTUAL, CaptureFrame.TYPE.getPath(), "setCell", "(" + Type.I + CaptureCell.TYPE + ")V", false);
        }
        mv.visitVarInsn(ALOAD, frameSlot);
    }

    @Override
    public void analyzeTypes(LambdaExpression result, TypeAnalyzer analyzer) {
        int lambdaLocalCount = result.getLocalVariables().size();
        int captureOffset = result.getCaptureOffset();
        // 使用独立的 TypeAnalyzer 扫描 Lambda 体，避免污染父函数的类型映射
        TypeAnalyzer lambdaScan = new TypeAnalyzer();
        lambdaScan.analyzeNode(result.getBody());
        // 位置 < captureOffset 或 >= lambdaLocalCount 的变量是从父作用域捕获的
        // 捕获的变量必须使用引用类型（getLocalRef/setLocalRef 支持环境链穿透）
        for (int pos : new HashSet<>(lambdaScan.getVariableTypes().keySet())) {
            if (pos < captureOffset || pos >= lambdaLocalCount) {
                analyzer.markCaptured(pos);
            }
        }
    }
}
