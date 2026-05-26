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
import org.tabooproject.fluxon.parser.expression.RangeExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.collection.IntRange;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.*;

public class RangeEvaluator extends ExpressionEvaluator<RangeExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.RANGE;
    }

    @Override
    public Type evaluate(Interpreter interpreter, RangeExpression result) {
        Type st = interpreter.evaluate(result.getStart());
        if (st.isPrimitive()) {
            int startInt = (int) interpreter.resultPrimitive;
            Type et = interpreter.evaluate(result.getEnd());
            if (et.isPrimitive()) {
                interpreter.resultRef = Intrinsics.createRange(startInt, (int) interpreter.resultPrimitive, result.isInclusive());
                return Type.OBJECT;
            }
            Object end = interpreter.resultRef;
            interpreter.resultRef = Intrinsics.createRange(startInt, end, result.isInclusive());
            return Type.OBJECT;
        }
        Object start = interpreter.resultRef;
        Type et = interpreter.evaluate(result.getEnd());
        Object end = interpreter.getResultBoxed(et);
        interpreter.resultRef = Intrinsics.createRange(start, end, result.isInclusive());
        return Type.OBJECT;
    }

    @Override
    public Type generateBytecode(RangeExpression result, CodeContext ctx, MethodVisitor mv) {
        Evaluator<ParseResult> startEval = ctx.getEvaluator(result.getStart());
        if (startEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for start expression");
        }
        Evaluator<ParseResult> endEval = ctx.getEvaluator(result.getEnd());
        if (endEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for end expression");
        }

        TypeAnalyzer analyzer = ctx.getTypeAnalyzer();
        Type startType = analyzer != null ? analyzer.inferType(result.getStart()) : Type.OBJECT;
        Type endType = analyzer != null ? analyzer.inferType(result.getEnd()) : Type.OBJECT;
        if (isPrimitiveNumericEndpoint(startType) && isPrimitiveNumericEndpoint(endType)) {
            // 只有栈上已是 primitive 数字时才走 primitive overload，动态 Object 值保留运行时错误边界。
            emitIntEndpoint(result.getStart(), startEval, ctx, mv);
            emitIntEndpoint(result.getEnd(), endEval, ctx, mv);
            mv.visitInsn(result.isInclusive() ? ICONST_1 : ICONST_0);
            mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "createRange", "(IIZ)" + IntRange.TYPE, false);
            return Type.OBJECT;
        }

        // 生成 start 表达式的字节码
        Type st = startEval.generateBytecode(result.getStart(), ctx, mv);
        if (st == Type.VOID) {
            throw new VoidError("Void type is not allowed for range start");
        }
        boxing(st, mv);
        // 生成 end 表达式的字节码
        Type et = endEval.generateBytecode(result.getEnd(), ctx, mv);
        if (et == Type.VOID) {
            throw new VoidError("Void type is not allowed for range end");
        }
        boxing(et, mv);
        // 压入 isInclusive 参数
        mv.visitInsn(result.isInclusive() ? ICONST_1 : ICONST_0);
        mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "createRange", "(" + Type.OBJECT + Type.OBJECT + "Z)" + IntRange.TYPE, false);
        return Type.OBJECT;
    }

    private static boolean isPrimitiveNumericEndpoint(Type type) {
        return type == Type.I || type == Type.J || type == Type.F || type == Type.D;
    }

    private static void emitIntEndpoint(ParseResult endpoint, Evaluator<ParseResult> evaluator, CodeContext ctx, MethodVisitor mv) {
        Type type = evaluator.generateBytecode(endpoint, ctx, mv);
        if (type == Type.VOID) {
            throw new VoidError("Void type is not allowed for range endpoint");
        }
        if (!type.isPrimitive()) {
            mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
            Instructions.emitNumberValue(mv, Type.I);
            return;
        }
        Instructions.emitPrimitiveConversion(type, Type.I, mv);
    }

    @Override
    public void analyzeTypes(RangeExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getStart());
        analyzer.analyzeNode(result.getEnd());
    }

    @Override
    public Type inferResultType(RangeExpression result, TypeAnalyzer analyzer) {
        // Range 始终产生 int 元素（Intrinsics.createRange 强制转 int）
        return IntRange.TYPE.withElementType(Type.I);
    }
}
