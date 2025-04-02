package org.tabooproject.fluxon.interpreter.evaluator;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.runtime.Type;

public interface Evaluator<T extends ParseResult> {

    /**
     * 评估结果
     */
    Object evaluate(Interpreter interpreter, T result);
    
    /**
     * 生成字节码
     *
     * @param result 解析结果
     * @param ctx    代码上下文
     * @param mv     方法访问器
     */
    Type generateBytecode(T result, CodeContext ctx, MethodVisitor mv);
}
