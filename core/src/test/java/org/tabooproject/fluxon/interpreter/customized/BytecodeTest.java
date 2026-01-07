package org.tabooproject.fluxon.interpreter.customized;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.type.TestRuntime;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class BytecodeTest {

    @BeforeEach
    public void BeforeEach() {
        TestRuntime.registerTestFunctions();
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
    public void test3() throws Exception {
        run(new File("src/test/fs/test3.fs"));
        compile(new File("src/test/fs/test3.fs"));
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
        result.dump(new File(file.getParentFile(), className + ".class"));
        // 加载并执行
        Class<?> scriptClass = result.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base = (RuntimeScriptBase) scriptClass.newInstance();
        // 使用注册中心初始化环境
        System.out.println("Result: " + base.eval(FluxonRuntime.getInstance().newEnvironment()));
    }
}
