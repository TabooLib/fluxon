package org.tabooproject.fluxon.compiler;

import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.Definitions;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

public class CompileResult {

    private final String source;
    private final String className;
    private final BytecodeGenerator generator;
    private final byte[] bytecode;

    public CompileResult(String source, String className, BytecodeGenerator generator, byte[] bytecode) {
        this.source = source;
        this.className = className;
        this.generator = generator;
        this.bytecode = bytecode;
    }

    public Environment newEnvironment() {
        Environment env = new Environment();
        FluxonRuntime.getInstance().initializeEnvironment(env);
        for (Definition definition : generator.getDefinitions()) {
            if (definition instanceof Definitions.FunctionDefinition) {
                Definitions.FunctionDefinition functionDefinition = (Definitions.FunctionDefinition) definition;
            }
        }
        return env;
    }

    public String getSource() {
        return source;
    }

    public String getClassName() {
        return className;
    }

    public BytecodeGenerator getGenerator() {
        return generator;
    }

    public byte[] getBytecode() {
        return bytecode;
    }
}
