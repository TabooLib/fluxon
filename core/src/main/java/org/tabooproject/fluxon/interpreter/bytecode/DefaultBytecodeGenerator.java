package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.bytecode.emitter.EmitResult;
import org.tabooproject.fluxon.interpreter.bytecode.emitter.FunctionClassEmitter;
import org.tabooproject.fluxon.interpreter.bytecode.emitter.MainClassEmitter;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 默认字节码生成器实现
 * 作为协调者，将具体的类生成工作委托给专门的 Emitter
 */
public class DefaultBytecodeGenerator implements BytecodeGenerator {

    // 存储脚本主体语句
    private final List<Statement> statements = new ArrayList<>();
    // 存储用户函数定义
    private final List<Definition> definitions = new ArrayList<>();
    // 收集到的 lambda 定义
    private List<LambdaFunctionDefinition> lastLambdaDefinitions = new ArrayList<>();
    // 收集到的 Command 解析数据
    private List<Object> lastCommandDataList = new ArrayList<>();
    // 脚本源上下文
    private String source = "";
    // 脚本文件名
    private String fileName = "main";

    @Override
    public Type generateExpressionBytecode(Expression expr, CodeContext ctx, MethodVisitor mv) {
        return expr.getExpressionType().evaluator.generateBytecode(expr, ctx, mv);
    }

    @Override
    public Type generateStatementBytecode(Statement stmt, CodeContext ctx, MethodVisitor mv) {
        return stmt.getStatementType().evaluator.generateBytecode(stmt, ctx, mv);
    }

    @Override
    public void addScriptBody(Statement... statements) {
        Collections.addAll(this.statements, statements);
    }

    @Override
    public void addScriptDefinition(Definition... definitions) {
        Collections.addAll(this.definitions, definitions);
    }

    @Override
    public void setSourceContext(String source, String fileName) {
        if (source != null) {
            this.source = source;
        }
        if (fileName != null && !fileName.isEmpty()) {
            this.fileName = fileName;
        }
    }

    @Override
    public List<byte[]> generateClassBytecode(String className, ClassLoader classLoader) {
        return generateClassBytecode(className, RuntimeScriptBase.TYPE.getPath(), classLoader);
    }

    @Override
    public List<byte[]> generateClassBytecode(String className, String superClassName, ClassLoader classLoader) {
        List<byte[]> byteList = new ArrayList<>();
        // 1. 生成主类
        MainClassEmitter mainEmitter = new MainClassEmitter(className, superClassName, fileName, source, statements, definitions, this, classLoader);
        EmitResult mainResult = mainEmitter.emit();
        byteList.add(mainResult.getBytecode());
        List<LambdaFunctionDefinition> lambdaDefinitions = new ArrayList<>(mainResult.getLambdaDefinitions());
        // 2. 为每个用户函数生成类
        for (Definition def : definitions) {
            if (def instanceof FunctionDefinition) {
                FunctionClassEmitter funcEmitter = new FunctionClassEmitter((FunctionDefinition) def, className, fileName, source, this, classLoader);
                EmitResult funcResult = funcEmitter.emit();
                byteList.add(funcResult.getBytecode());
                lambdaDefinitions.addAll(funcResult.getLambdaDefinitions());
            }
        }
        // 3. 为每个 Lambda 生成类（注意：Lambda 可能嵌套 Lambda）
        for (int i = 0; i < lambdaDefinitions.size(); i++) {
            LambdaFunctionDefinition lambdaDef = lambdaDefinitions.get(i);
            FunctionClassEmitter lambdaEmitter = new FunctionClassEmitter(lambdaDef, lambdaDef.getOwnerClassName(), fileName, source, this, classLoader);
            EmitResult lambdaResult = lambdaEmitter.emit();
            byteList.add(lambdaResult.getBytecode());
            // Lambda 可能嵌套 Lambda，添加到列表末尾继续处理
            lambdaDefinitions.addAll(lambdaResult.getLambdaDefinitions());
        }
        // 保存结果供后续查询
        this.lastLambdaDefinitions = lambdaDefinitions;
        if (mainResult.getCtx() != null) {
            this.lastCommandDataList = mainResult.getCtx().getCommandDataList();
        }
        return byteList;
    }

    @Override
    public List<Statement> getStatements() {
        return statements;
    }

    @Override
    public List<Definition> getDefinitions() {
        return definitions;
    }

    @Override
    public List<LambdaFunctionDefinition> getLambdaDefinitions() {
        return lastLambdaDefinitions;
    }

    @Override
    public List<Object> getCommandDataList() {
        return lastCommandDataList;
    }
}
