package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.StringInterpolation;
import org.tabooproject.fluxon.runtime.Type;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.OBJECT;
import static org.tabooproject.fluxon.runtime.Type.STRING;

/**
 * 字符串插值表达式求值器
 * 将 StringInterpolation 求值为拼接后的字符串
 */
public class StringInterpolationEvaluator extends ExpressionEvaluator<StringInterpolation> {

    private static final Type STRING_BUILDER = new Type(StringBuilder.class);

    @Override
    public ExpressionType getType() {
        return ExpressionType.STRING_INTERPOLATION;
    }

    @Override
    public Object evaluate(Interpreter interpreter, StringInterpolation expr) {
        StringBuilder result = new StringBuilder();
        for (ParseResult part : expr.getParts()) {
            if (part instanceof StringInterpolation.StringPart) {
                result.append(((StringInterpolation.StringPart) part).getValue());
            } else {
                Object value = interpreter.evaluate(part);
                result.append(value == null ? "null" : value.toString());
            }
        }
        return result.toString();
    }

    @Override
    public Type generateBytecode(StringInterpolation expr, CodeContext ctx, MethodVisitor mv) {
        List<ParseResult> parts = expr.getParts();
        // 创建 StringBuilder
        mv.visitTypeInsn(NEW, STRING_BUILDER.getPath());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, STRING_BUILDER.getPath(), "<init>", "()V", false);
        // 遍历所有部分，依次 append
        for (ParseResult part : parts) {
            if (part instanceof StringInterpolation.StringPart) {
                // 字符串片段直接 append
                String value = ((StringInterpolation.StringPart) part).getValue();
                mv.visitLdcInsn(value);
                mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.getPath(), "append", "(" + STRING + ")" + STRING_BUILDER, false);
            } else {
                // 表达式：求值后转为字符串再 append
                Evaluator<ParseResult> evaluator = ctx.getEvaluator(part);
                if (evaluator == null) {
                    throw new RuntimeException("No evaluator found for interpolation part: " + part);
                }
                Type exprType = evaluator.generateBytecode(part, ctx, mv);
                // 如果是基本类型，需要装箱
                if (exprType.isPrimitive()) {
                    boxing(exprType, mv);
                }
                // 调用 String.valueOf 转换为字符串
                mv.visitMethodInsn(INVOKESTATIC, STRING.getPath(), "valueOf", "(" + OBJECT + ")" + STRING, false);
                mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.getPath(), "append", "(" + STRING + ")" + STRING_BUILDER, false);
            }
        }
        // 调用 toString
        mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.getPath(), "toString", "()" + STRING, false);
        return STRING;
    }
}
