package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.Definitions;
import org.tabooproject.fluxon.runtime.Environment;

/**
 * 定义求值器
 * 处理所有定义类型的求值
 */
public class DefinitionVisitor extends AbstractVisitor {

    public DefinitionVisitor(Interpreter interpreter, Environment environment) {
        super(interpreter, environment);
    }

    @Override
    public Object visitDefinition(Definition definition) {
        if (definition instanceof Definitions.FunctionDefinition) {
            return evaluateFunctionDefinition((Definitions.FunctionDefinition) definition);
        }
        throw new RuntimeException("Unknown definition type: " + definition.getClass().getName());
    }

    private UserFunction evaluateFunctionDefinition(Definitions.FunctionDefinition funcDef) {
        // 创建函数对象，捕获当前环境
        UserFunction function = new UserFunction(funcDef, interpreter.getEnvironment(), interpreter);
        // 在当前环境中定义函数
        interpreter.getEnvironment().defineRootFunction(funcDef.getName(), function);
        return function;
    }
}