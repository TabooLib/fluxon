package org.tabooproject.fluxon.compiler;

import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * 编译结果，封装主类与内部函数类的字节码
 */
public class CompileResult {

    private final String source;
    private final String className;
    private final BytecodeGenerator generator;
    private final byte[] mainClass;
    private final List<byte[]> innerClasses;

    public CompileResult(String source,
                         String className,
                         BytecodeGenerator generator,
                         List<byte[]> bytecode) {
        this.source = source;
        this.className = className;
        this.generator = generator;
        this.mainClass = bytecode.get(0);
        this.innerClasses = bytecode.subList(1, bytecode.size());
    }

    /**
     * 将编译结果定义到指定的类加载器中
     *
     * @param loader 类加载器
     * @return 主脚本类
     */
    public Class<?> defineClass(FluxonClassLoader loader) {
        Class<?> scriptClass = loader.defineClass(className, getMainClass());
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
        for (LambdaFunctionDefinition lambdaDef : generator.getLambdaDefinitions()) {
            String lambdaClassName = lambdaDef.getOwnerClassName() + lambdaDef.getName();
            if (i < innerClasses.size()) {
                loader.defineClass(lambdaClassName, innerClasses.get(i));
                i++;
            }
        }
        return scriptClass;
    }

    /**
     * 创建主脚本类的实例
     *
     * @param loader 类加载器
     * @return 主脚本类实例
     * @throws Exception 若实例化失败
     */
    public Object createInstance(FluxonClassLoader loader) throws Exception {
        Class<?> scriptClass = defineClass(loader);
        return scriptClass.getDeclaredConstructor().newInstance();
    }

    /**
     * 将字节码写入文件，便于调试
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
        for (LambdaFunctionDefinition lambdaDef : generator.getLambdaDefinitions()) {
            String lambdaClassName = lambdaDef.getOwnerClassName() + lambdaDef.getName();
            if (i < innerClasses.size()) {
                File compiled = new File(file.getParentFile(), lambdaClassName + ".class");
                compiled.createNewFile();
                Files.write(compiled.toPath(), innerClasses.get(i));
                i++;
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

