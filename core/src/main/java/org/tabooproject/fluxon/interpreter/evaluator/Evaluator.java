package org.tabooproject.fluxon.interpreter.evaluator;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.runtime.Type;

public abstract class Evaluator<T extends ParseResult> {

    /**
     * 评估结果，返回 Type 标识结果类型
     * 结果通过 interpreter.resultRef / resultPrimitive 传递
     */
    abstract public Type evaluate(Interpreter interpreter, T result);

    /**
     * 生成字节码
     *
     * @param result 解析结果
     * @param ctx    代码上下文
     * @param mv     方法访问器
     */
    abstract public Type generateBytecode(T result, CodeContext ctx, MethodVisitor mv);

    /**
     * 遍历子节点收集赋值类型信息
     * 需要递归遍历的 Evaluator 应重写此方法
     */
    public void analyzeTypes(T result, TypeAnalyzer analyzer) {
        // 默认不做任何事
    }

    /**
     * 推断该表达式的结果类型
     * 需要类型推断的 Evaluator 应重写此方法
     */
    public Type inferResultType(T result, TypeAnalyzer analyzer) {
        return Type.OBJECT;
    }

    /**
     * 将操作数装箱
     *
     * @param type 类型
     * @return 装箱后的类型
     */
    protected static Type boxing(Type type, MethodVisitor mv) {
        Instructions.emitBox(mv, type);
        if (type == Type.I) return Type.INT;
        if (type == Type.J) return Type.LONG;
        if (type == Type.F) return Type.FLOAT;
        if (type == Type.D) return Type.DOUBLE;
        if (type == Type.Z) return Type.BOOLEAN;
        return type;
    }
}
