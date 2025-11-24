package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.UserFunction;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.LambdaExpression;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;

/**
 * Lambda 表达式求值/生成
 */
public class LambdaEvaluator extends ExpressionEvaluator<LambdaExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.LAMBDA;
    }

    @Override
    public Object evaluate(Interpreter interpreter, LambdaExpression expr) {
        return new UserFunction(expr.toFunctionDefinition("main"), interpreter);
    }

    @Override
    public Type generateBytecode(LambdaExpression result, CodeContext ctx, MethodVisitor mv) {
        LambdaFunctionDefinition definition = (LambdaFunctionDefinition) result.toFunctionDefinition(ctx.getClassName());
        ctx.addLambdaDefinition(definition);
        String lambdaClassName = definition.getOwnerClassName() + definition.getName();
        mv.visitFieldInsn(GETSTATIC, ctx.getClassName(), definition.getName(), "L" + lambdaClassName + ";");
        return Function.TYPE;
    }
}