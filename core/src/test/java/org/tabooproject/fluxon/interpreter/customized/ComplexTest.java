package org.tabooproject.fluxon.interpreter.customized;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.type.TestRuntime;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class ComplexTest {

    public static void main(String[] args) throws Exception {
        new ComplexTest().benchmarkInterpret();
    }

    private String loadEffectSource() throws Exception {
        File fsFile = new File("effect.fs");
        if (!fsFile.exists()) fsFile = new File("../effect.fs");
        List<String> lines = Files.readAllLines(fsFile.toPath());
        return String.join("\n", lines).trim();
    }

    private Environment createEnv() {
        TestRuntime.registerTestFunctions();
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("audience", new TestRuntime.TestAudience(new TestRuntime.TestLocation(0, 0, 0)));
        env.defineRootVariable("location", new TestRuntime.TestLocation(0, 0, 0));
        return env;
    }

    private CompilationContext createContext(String source) {
        CompilationContext context = new CompilationContext(source);
        context.defineRootVariable("audience", TestRuntime.TestAudience.class);
        context.defineRootVariable("location", TestRuntime.TestLocation.class);
        return context;
    }

    private void runBenchmark(String label, Runnable action) {
        // 预热
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 1000; j++) {
                action.run();
            }
        }
        // 测量
        int rounds = 20;
        long[] times = new long[rounds];
        for (int i = 0; i < rounds; i++) {
            long time = System.currentTimeMillis();
            for (int j = 0; j < 1000; j++) {
                action.run();
            }
            times[i] = System.currentTimeMillis() - time;
            System.out.println("[" + label + "] Round " + i + ": " + times[i] + "ms");
        }
        // 取后 10 轮平均值
        long sum = 0;
        for (int i = rounds / 2; i < rounds; i++) {
            sum += times[i];
            System.out.println("[" + label + "] Round " + i + ": " + times[i] + "ms");
        }
        System.out.println("[" + label + "] Average (last " + (rounds / 2) + " rounds): " + (sum / (rounds / 2)) + "ms / 1000 evals");
    }

    public void benchmarkInterpret() throws Exception {
        Environment env = createEnv();
        String source = loadEffectSource();
        CompilationContext context = createContext(source);
        ParsedScript script = Fluxon.parse(context, env);
        runBenchmark("Interpret", () -> script.eval(env));
    }

    public void benchmarkCompile() throws Exception {
        Environment env = createEnv();
        String source = loadEffectSource();
        CompilationContext context = createContext(source);
        CompileResult result = Fluxon.compile(env, context, "EffectBenchmark");
        Class<?> scriptClass = result.defineClass(new FluxonClassLoader());
        RuntimeScriptBase script = (RuntimeScriptBase) scriptClass.getDeclaredConstructor().newInstance();
        Object[] cmdData = result.getCommandDataArray();
        if (cmdData != null) {
            script.setCommandData(cmdData);
        }
        runBenchmark("Compile", () -> script.eval(env));
    }
}
