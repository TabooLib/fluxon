package org.tabooproject.fluxon.compiler;

import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;

import java.io.File;
import java.nio.file.Files;
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

    /**
     * 定义编译结果的类
     * 主类将被定义为指定的类名，用户函数类将被定义为 className + 函数名
     *
     * @param loader 类加载器
     * @return 脚本类
     */
    public Class<?> defineClass(FluxonClassLoader loader) {
        // 定义主类
        Class<?> scriptClass = loader.defineClass(className, getMainClass());
        // 定义用户函数类
        int i = 0;
        for (Definition definition : generator.getDefinitions()) {
            if (definition instanceof FunctionDefinition) {
                FunctionDefinition funcDef = (FunctionDefinition) definition;
                String functionClassName = className + funcDef.getName();
                if (i < innerClasses.size()) {
                    loader.defineClass(functionClassName, innerClasses.get(i));
                    i++;
                }
            }
        }
        return scriptClass;
    }

    /**
     * 输出编译结果到文件
     * 主类将输出到指定文件，用户函数类将输出到与主类相同的目录下
     *
     * @param file 输出文件
     */
    public void dump(File file) throws Exception {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        Files.write(file.toPath(), mainClass);
        int i = 0;
        for (Definition definition : getGenerator().getDefinitions()) {
            if (definition instanceof FunctionDefinition) {
                FunctionDefinition funcDef = (FunctionDefinition) definition;
                String functionClassName = className + funcDef.getName();
                if (i < innerClasses.size()) {
                    File compiled = new File(file.getParentFile(), functionClassName + ".class");
                    compiled.createNewFile();
                    Files.write(compiled.toPath(), innerClasses.get(i));
                    i++;
                }
            }
        }
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
