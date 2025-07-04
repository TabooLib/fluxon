package org.tabooproject.fluxon.interpreter.bytecode;

import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.Definitions;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class BytecodeTest {

    public static void main(String[] args) {
        // 从 build 里获取所有结尾为 .fs 的文件
        File file = new File("test");
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".fs")) {
                    // 编译文件
                    try {
                        run(f);
                        compile(f);
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public static void run(File file) throws Exception {
        System.out.println("---------------------------------");
        System.out.println("Running: " + file);
        List<String> strings = Files.readAllLines(file.toPath());
        Object result = Fluxon.eval(String.join("\n", strings));
        System.out.println("Result: " + result);
    }

    public static void compile(File file) throws Exception {
        System.out.println("---------------------------------");
        System.out.println("Compiling: " + file);
        List<String> strings = Files.readAllLines(file.toPath());
        String className = file.getName().replace(".fs", "");
        CompileResult result = Fluxon.compile(String.join("\n", strings), className);

        // 输出脚本便于调试
        File compiled = new File(file.getParentFile(), className + ".class");
        compiled.createNewFile();
        Files.write(compiled.toPath(), result.getMainClass());
        
        // 输出用户函数类
        int i = 0;
        for (Definition definition : result.getGenerator().getDefinitions()) {
            if (definition instanceof Definitions.FunctionDefinition) {
                Definitions.FunctionDefinition funcDef = (Definitions.FunctionDefinition) definition;
                String functionClassName = className + funcDef.getName();
                if (i < result.getInnerClasses().size()) {
                    compiled = new File(file.getParentFile(), functionClassName + ".class");
                    compiled.createNewFile();
                    Files.write(compiled.toPath(), result.getInnerClasses().get(i));
                    i++;
                }
            }
        }

        // 加载并执行
        Class<?> scriptClass = result.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base = (RuntimeScriptBase) scriptClass.newInstance();

        // 使用注册中心初始化环境
        System.out.println("Result: " + base.eval(FluxonRuntime.getInstance().newEnvironment()));
    }
}
