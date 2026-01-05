package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.parser.DomainExecutor;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.DomainExpression;
import org.tabooproject.fluxon.parser.expression.LambdaExpression;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.Collections;
import java.util.LinkedHashMap;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.OBJECT;
import static org.tabooproject.fluxon.runtime.Type.STRING;

/**
 * Domain 表达式求值器
 * <p>
 * 执行域表达式的逻辑。域执行器接收运行时环境和域体闭包，
 * 可以完全控制域体的执行时机和方式。
 *
 * <h3>执行流程</h3>
 * <ol>
 *   <li>获取解析时捕获的 {@link DomainExecutor}</li>
 *   <li>将域体包装为 Supplier</li>
 *   <li>调用 executor.execute(environment, body)</li>
 *   <li>返回执行器的返回值</li>
 * </ol>
 *
 * @see DomainExpression
 * @see DomainExecutor
 */
public class DomainEvaluator extends Evaluator<DomainExpression> {

    @Override
    public Object evaluate(Interpreter interpreter, DomainExpression expr) {
        try {
            DomainExecutor executor = expr.getExecutor();
            ParseResult bodyAst = expr.getBody();
            return executor.execute(interpreter.getEnvironment(), () -> interpreter.evaluate(bodyAst));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Error executing domain '" + expr.getDomainName() + "': " + ex.getMessage(), ex);
        }
    }

    @Override
    public Type generateBytecode(DomainExpression expr, CodeContext ctx, MethodVisitor mv) {
        String domainName = expr.getDomainName();
        // 1. 将 domain body 编译为一个无参 lambda 函数
        String lambdaName = "$domain$" + domainName + "$" + System.identityHashCode(expr);
        LambdaExpression bodyLambda = new LambdaExpression(
            lambdaName,
            new LinkedHashMap<>(),  // 无参数
            expr.getBody(),
            Collections.emptySet()  // 无局部变量（继承外部作用域）
        );
        LambdaFunctionDefinition lambdaDef = (LambdaFunctionDefinition) bodyLambda.toFunctionDefinition(ctx.getClassName());
        ctx.addLambdaDefinition(lambdaDef);
        String lambdaClassName = lambdaDef.getOwnerClassName() + lambdaDef.getName();
        // 1. 加载 domain name
        mv.visitLdcInsn(domainName);
        // 2. 加载 environment
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());
        // 3. 加载 body function
        mv.visitFieldInsn(GETSTATIC, ctx.getClassName(), lambdaDef.getName(), "L" + lambdaClassName + ";");
        // 调用 Intrinsics.executeDomain(String, Environment, Function)
        mv.visitMethodInsn(
            INVOKESTATIC,
            Intrinsics.TYPE.getPath(),
            "executeDomain",
            "(" + STRING + Environment.TYPE + Function.TYPE + ")" + OBJECT,
            false
        );
        return OBJECT;
    }
}
