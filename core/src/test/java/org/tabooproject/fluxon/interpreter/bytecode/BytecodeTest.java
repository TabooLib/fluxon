package org.tabooproject.fluxon.interpreter.bytecode;

import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

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
                        compile(f);
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public static void compile(File file) throws Exception {
        List<String> strings = Files.readAllLines(file.toPath());
        String className = file.getName().replace(".fs", "");
        CompileResult result = Fluxon.compile(String.join("\n", strings), className);

        // 输出脚本便于调试
        File compiled = new File(file.getParentFile(), className + ".class");
        compiled.createNewFile();
        Files.write(compiled.toPath(), result.getBytecode());

        // 加载并执行
        FluxonClassLoader loader = new FluxonClassLoader();
        Class<?> scriptClass = loader.defineClass(className, result.getBytecode());
        RuntimeScriptBase base = (RuntimeScriptBase) scriptClass.newInstance();
        System.out.println(file + " Result:");

        // 使用注册中心初始化环境
        System.out.println(base.eval(result.newEnvironment()));
    }
}
