package org.tabooproject.fluxon.interpreter.evaluator.expr.assign;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.AssignExpression;

/**
 * 赋值目标处理器
 *
 * @author sky
 */
public interface AssignmentTargetHandler<T extends ParseResult> {

    /**
     * 解释执行赋值
     */
    void assign(Interpreter interpreter, AssignExpression expr, T target, Object value, TokenType op);

    /**
     * 生成赋值字节码
     */
    void generateBytecode(AssignExpression expr, T target, Evaluator<ParseResult> valueEval, CodeContext ctx, MethodVisitor mv);
}
