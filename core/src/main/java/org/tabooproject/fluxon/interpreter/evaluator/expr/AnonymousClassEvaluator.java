package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AnonymousClassExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.OBJECT;

/**
 * 匿名类表达式求值器
 * 仅支持编译模式
 *
 * @author sky
 */
public class AnonymousClassEvaluator extends ExpressionEvaluator<AnonymousClassExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.ANONYMOUS_CLASS;
    }

    @Override
    public Object evaluate(Interpreter interpreter, AnonymousClassExpression expression) {
        throw new UnsupportedOperationException("Anonymous class expressions are only supported in compilation mode");
    }

    @Override
    public Type generateBytecode(AnonymousClassExpression expression, CodeContext ctx, MethodVisitor mv) {
        // 获取当前匿名类索引（在添加之前获取，保证顺序一致）
        int index = ctx.getAnonymousClassIndex();
        String anonymousClassName = ctx.getClassName() + "$" + index;
        // 将表达式添加到 CodeContext，供后续 Emitter 处理
        ctx.addAnonymousClassDefinition(expression);
        ctx.incrementAnonymousClassIndex();
        // 生成 NEW + DUP
        mv.visitTypeInsn(NEW, anonymousClassName);
        mv.visitInsn(DUP);
        // 加载 environment
        Instructions.loadEnvironment(mv, ctx);
        // 处理构造参数
        List<ParseResult> superArgs = expression.getSuperArgs();
        if (superArgs != null && !superArgs.isEmpty()) {
            // 创建 Object[] 数组
            mv.visitLdcInsn(superArgs.size());
            mv.visitTypeInsn(ANEWARRAY, OBJECT.getPath());
            // 填充参数
            for (int i = 0; i < superArgs.size(); i++) {
                mv.visitInsn(DUP);
                mv.visitLdcInsn(i);
                ParseResult arg = superArgs.get(i);
                Evaluator<ParseResult> argEval = ctx.getEvaluator(arg);
                Type argType = argEval.generateBytecode(arg, ctx, mv);
                if (argType.isPrimitive()) {
                    boxing(argType, mv);
                }
                mv.visitInsn(AASTORE);
            }
            // 调用构造函数: (Environment, Object[])
            mv.visitMethodInsn(INVOKESPECIAL, anonymousClassName, "<init>", "(" + Environment.TYPE + "[" + OBJECT + ")V", false);
        } else {
            // 调用构造函数: (Environment)
            mv.visitMethodInsn(INVOKESPECIAL, anonymousClassName, "<init>", "(" + Environment.TYPE + ")V", false);
        }
        return OBJECT;
    }
}
