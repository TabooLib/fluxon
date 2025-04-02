package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;

/**
 * 字节码生成器接口
 */
public interface BytecodeGenerator {
    
    /**
     * 为表达式生成字节码
     */
    void generateExpressionBytecode(Expression expr, MethodVisitor mv);
    
    /**
     * 为语句生成字节码
     */  
    void generateStatementBytecode(Statement stmt, MethodVisitor mv);
    
    /**
     * 添加一个字段
     * @param name 字段名
     * @param type 字段类型描述符
     * @param initializer 初始化表达式
     */
    void addField(String name, String type, Expression initializer);
    
    /**
     * 设置脚本的返回值表达式
     * @param expr 返回值表达式
     */
    void setReturnExpression(Expression expr);
    
    /**
     * 添加脚本主体代码
     * @param statements 语句列表
     */
    void addScriptBody(Statement... statements);
    
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