package org.tabooproject.fluxon.interpreter.customized;

import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.type.TestRuntime;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@SuppressWarnings("deprecation")
public class ComplexTest {

    public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException {
        TestRuntime.registerTestFunctions();
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("audience", new TestRuntime.TestAudience(new TestRuntime.TestLocation(0, 0, 0)));
        env.defineRootVariable("location", new TestRuntime.TestLocation(0, 0, 0));
        List<String> lines = Files.readAllLines(new File("effect.fs").toPath());

        System.out.println("Run:");
        ParsedScript script = Fluxon.parse(String.join("\n", lines).trim(), env);
        script.eval(env);

        System.out.println("Compile:");
        CompileResult effect = Fluxon.compile(String.join("\n", lines).trim(), "effect", env);
        Class<?> defineClass = effect.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base = (RuntimeScriptBase) defineClass.newInstance();
        base.eval(env);

        for (int i = 0; i < 15; i++) {
            long time = System.currentTimeMillis();
            for (int j = 0; j < 1000; j++) {
                base.eval(env);
//                script.eval(env);
            }
            System.out.println((System.currentTimeMillis() - time) + "ms");
        }

        for (int i = 0; i < 10; i++) {
            long time = System.currentTimeMillis();
            base.eval(env);
//            script.eval(env);
            System.out.println((System.currentTimeMillis() - time) + "ms");
        }
    }
}
