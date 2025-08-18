package org.tabooproject.fluxon.interpreter.bytecode;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonRuntimeTest;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.Definitions;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class BytecodeTest {

    @BeforeAll
    public static void setup() {
        FluxonRuntimeTest.registerTestFunctions();
    }

    @Test
    public void test1() throws Exception {
        run(new File("src/test/fs/test1.fs"));
        compile(new File("src/test/fs/test1.fs"));
    }

    @Test
    public void test2() throws Exception {
        run(new File("src/test/fs/test2.fs"));
        compile(new File("src/test/fs/test2.fs"));
    }

    @Test
    public void test4() throws Exception {
        run(new File("src/test/fs/test4.fs"));
        compile(new File("src/test/fs/test4.fs"));
    }

    @Test
    public void test5() throws Exception {
        run(new File("src/test/fs/test5.fs"));
        compile(new File("src/test/fs/test5.fs"));
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
