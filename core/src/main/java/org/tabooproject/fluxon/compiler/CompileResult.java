package org.tabooproject.fluxon.compiler;

import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.Definitions;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.List;

public class CompileResult {

    private final String source;
    private final String className;
    private final BytecodeGenerator generator;
    private final byte[] mainClass;
    private final List<byte[]> innerClasses;

    public CompileResult(
            String source,
            String className,
            BytecodeGenerator generator,
            List<byte[]> bytecode
    ) {
        this.source = source;
        this.className = className;
        this.generator = generator;
        this.mainClass = bytecode.get(0);
        this.innerClasses = bytecode.subList(1, bytecode.size());
    }

    public Class<?> defineClass(FluxonClassLoader loader) {
        Class<?> scriptClass = loader.defineClass(className, getMainClass());
        int i = 0;
        for (byte[] innerClass : getInnerClasses()) {
            String innerClassName = className + "$" + i++;
            loader.defineClass(innerClassName, innerClass);
        }
        return scriptClass;
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

    public byte[] getMainClass() {
        return mainClass;
    }

    public List<byte[]> getInnerClasses() {
        return innerClasses;
    }
}
