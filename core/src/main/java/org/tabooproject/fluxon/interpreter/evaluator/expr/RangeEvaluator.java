package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.RangeExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.*;

public class RangeEvaluator extends ExpressionEvaluator<RangeExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.RANGE;
    }

    @Override
    public Object evaluate(Interpreter interpreter, RangeExpression result) {
        // 获取开始值和结束值
        Object start = interpreter.evaluate(result.getStart());
        Object end = interpreter.evaluate(result.getEnd());
        // 使用 Operations 类创建范围，保持与字节码生成的一致性
        return Intrinsics.createRange(start, end, result.isInclusive());
    }

    @Override
    public Type generateBytecode(RangeExpression result, CodeContext ctx, MethodVisitor mv) {
        // 生成 start 表达式的字节码
        Evaluator<ParseResult> startEval = ctx.getEvaluator(result.getStart());
        if (startEval == null) {
            throw new RuntimeException("No evaluator found for start expression");
        }
        if (startEval.generateBytecode(result.getStart(), ctx, mv) == Type.VOID) {
            throw new RuntimeException("Void type is not allowed for range start");
        }
        // 生成 end 表达式的字节码
        Evaluator<ParseResult> endEval = ctx.getEvaluator(result.getEnd());
        if (endEval == null) {
            throw new RuntimeException("No evaluator found for end expression");
        }
        if (endEval.generateBytecode(result.getEnd(), ctx, mv) == Type.VOID) {
            throw new RuntimeException("Void type is not allowed for range end");
        }
        // 压入 isInclusive 参数
        mv.visitInsn(result.isInclusive() ? ICONST_1 : ICONST_0);
        // 调用 Operations.createRange 方法
        mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "createRange", "(" + Type.OBJECT + Type.OBJECT + "Z)Ljava/util/List;", false);
        return Type.OBJECT;
    }
}
