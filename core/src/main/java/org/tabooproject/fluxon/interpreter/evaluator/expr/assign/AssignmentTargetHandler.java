package org.tabooproject.fluxon.interpreter.evaluator.expr.assign;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.runtime.Type;

/**
 * 赋值目标处理器
 *
 * @author sky
 */
public interface AssignmentTargetHandler<T extends ParseResult> {

    /**
     * 解释执行赋值
     * <p>
     * 值类型通过 vt 传入，handler 按需从 interpreter.resultPrimitive/resultRef 读取值，
     * 避免在不需要 Object 的路径上进行装箱。
     */
    void assign(Interpreter interpreter, AssignExpression expr, T target, Type vt, TokenType op);

    /**
     * 生成赋值字节码
     */
    void generateBytecode(AssignExpression expr, T target, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv);
}
