package org.tabooproject.fluxon.interpreter.evaluator;

import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.parser.ParseResult;
import org.objectweb.asm.MethodVisitor;

public interface Evaluator<T extends ParseResult> {

    /**
     * 评估结果
     */
    Object evaluate(Interpreter interpreter, T result);
    
    /**
     * 生成字节码
     * @param result 解析结果
     * @param mv 方法访问器
     */
    void generateBytecode(T result, MethodVisitor mv);
}
