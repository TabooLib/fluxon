package org.tabooproject.fluxon.interpreter.bytecode;

import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.interpreter.evaluator.StatementEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.parser.statement.StatementType;
import org.tabooproject.fluxon.runtime.Type;

import java.util.ArrayList;
import java.util.List;

public class CodeContext {

    // 获取评估器注册表
    private final EvaluatorRegistry registry = EvaluatorRegistry.getInstance();

    // 类名和父类名
    private final String className;
    private final String superClassName;

    // 用户定义
    private final List<Definition> definitions = new ArrayList<>();

    // 局部变量表
    private int localVarIndex = 0;

    public CodeContext(String className, String superClassName) {
        this.className = className;
        this.superClassName = superClassName;
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public CodeContext(CodeContext parent) {
        this.className = parent.className;
        this.superClassName = parent.superClassName;
        this.definitions.addAll(parent.definitions);
    }

    public void addDefinition(Definition definition) {
        definitions.add(definition);
    }

    public void addDefinitions(List<Definition> definitions) {
        this.definitions.addAll(definitions);
    }

    public String getClassName() {
        return className;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public List<Definition> getDefinitions() {
        return definitions;
    }

    public int allocateLocalVar(Type type) {
        String descriptor = type.getDescriptor();
        // 根据类型增加索引
        switch (descriptor) {
            case "J":
            case "D":
                localVarIndex += 2;
                break;
            default:
                localVarIndex += 1;
                break;
        }
        return localVarIndex;
    }

    public int getLocalVarIndex() {
        return localVarIndex;
    }

    public Evaluator<ParseResult> getEvaluator(ParseResult result) {
        return registry.getEvaluator(result);
    }

    public StatementEvaluator<Statement> getStatement(StatementType result) {
        return registry.getStatement(result);
    }

    public ExpressionEvaluator<Expression> getExpression(ExpressionType result) {
        return registry.getExpression(result);
    }
}
