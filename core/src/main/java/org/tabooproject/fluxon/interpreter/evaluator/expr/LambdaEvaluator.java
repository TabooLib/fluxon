package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.interpreter.UserFunction;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.LambdaExpression;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;

/**
 * Lambda 表达式求值
 */
public class LambdaEvaluator extends ExpressionEvaluator<LambdaExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.LAMBDA;
    }

    @Override
    public Object evaluate(Interpreter interpreter, LambdaExpression expr) {
        // Lambda 不注册到环境，直接返回函数实例
        return new UserFunction(expr.toFunctionDefinition("main"), interpreter);
    }

    @Override
    public Type generateBytecode(LambdaExpression result, CodeContext ctx, MethodVisitor mv) {
        LambdaFunctionDefinition definition = (LambdaFunctionDefinition) result.toFunctionDefinition(ctx.getClassName());
        ctx.addLambdaDefinition(definition);
        String lambdaClassName = definition.getOwnerClassName() + definition.getName();
        mv.visitTypeInsn(NEW, lambdaClassName);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, lambdaClassName, "<init>", "()V", false);
        return Function.TYPE;
    }
}
