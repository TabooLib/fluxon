package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Type;

import java.util.List;

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
     * 传入源代码与文件名，便于生成行号与错误修饰
     */
    void setSourceContext(String source, String fileName);

    /**
     * 设置根层级局部变量数量（_ 前缀变量）
     */
    void setRootLocalVariableCount(int count);

    /**
     * 获取根层级局部变量数量
     */
    int getRootLocalVariableCount();

    /**
     * 生成完整的类字节码
     */
    List<byte[]> generateClassBytecode(String className, ClassLoader classLoader);

    /**
     * 生成完整的类字节码
     * @param superClassName 父类名称
     */
    List<byte[]> generateClassBytecode(String className, String superClassName, ClassLoader classLoader);

    /**
     * 获取已生成的语句列表
     */
    List<Statement> getStatements();

    /**
     * 获取已生成的定义列表
     */
    List<Definition> getDefinitions();

     /**
      * 获取已生成的 Lambda 函数定义列表
      */
    List<LambdaFunctionDefinition> getLambdaDefinitions();

    /**
     * 获取 Command 解析数据列表
     */
    List<Object> getCommandDataList();
}
