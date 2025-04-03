package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Type;

/**
 * 字节码生成器接口
 */
public interface BytecodeGenerator {
    
    /**
     * 为表达式生成字节码
     */
    Type generateExpressionBytecode(Expression expr, CodeContext ctx, MethodVisitor mv);
    
    /**
     * 为语句生成字节码
     */  
    Type generateStatementBytecode(Statement stmt, CodeContext ctx, MethodVisitor mv);
    
    /**
     * 添加脚本主体代码
     * @param statements 语句列表
     */
    void addScriptBody(Statement... statements);

    /**
     * 添加脚本定义代码
     */
    void addScriptDefinition(Definition... definitions);

    /**
     * 生成完整的类字节码
     */
    byte[] generateClassBytecode(String className);

    /**
     * 生成完整的类字节码
     * @param superClassName 父类名称
     */
    byte[] generateClassBytecode(String className, String superClassName);
}