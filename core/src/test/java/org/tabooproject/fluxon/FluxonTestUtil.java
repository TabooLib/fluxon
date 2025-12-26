package org.tabooproject.fluxon;

import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class FluxonTestUtil {

    /**
     * 测试结果类
     */
    public static class TestResult {

        private final Object interpretResult;
        private final Environment interpretEnv;
        private final Object compileResult;
        private final Environment compileEnv;
        private final long interpretTime;
        private final long compileTime;
        private final long executeTime;

        public TestResult(Object interpretResult, Environment interpretEnv, Object compileResult, Environment compileEnv, long interpretTime, long compileTime, long executeTime) {
            this.interpretResult = interpretResult;
            this.interpretEnv = interpretEnv;
            this.compileResult = compileResult;
            this.compileEnv = compileEnv;
            this.interpretTime = interpretTime;
            this.compileTime = compileTime;
            this.executeTime = executeTime;
        }

        public Object getInterpretResult() {
            return interpretResult;
        }

        public Environment getInterpretEnv() {
            return interpretEnv;
        }

        public Object getCompileResult() {
            return compileResult;
        }

        public Environment getCompileEnv() {
            return compileEnv;
        }

        public long getInterpretTime() {
            return interpretTime;
        }

        public long getCompileTime() {
            return compileTime;
        }

        public long getExecuteTime() {
            return executeTime;
        }

        @Override
        public String toString() {
            return "TestResult{\n" +
                    "  interpretResult=" + interpretResult + ",\n" +
                    "  compileResult=" + compileResult + ",\n" +
                    "  interpretTime=" + interpretTime + "ms,\n" +
                    "  compileTime=" + compileTime + "ms,\n" +
                    "  executeTime=" + executeTime + "ms\n" +
                    "}";
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("interpretResult", interpretResult);
            map.put("compileResult", compileResult);
            map.put("interpretTime", interpretTime);
            map.put("compileTime", compileTime);
            map.put("executeTime", executeTime);
            map.put("match", isMatch());
            return map;
        }

        public boolean isMatch() {
            if (interpretResult == null && compileResult == null) {
                return true;
            }
            if (interpretResult == null || compileResult == null) {
                return false;
            }
            return interpretResult.equals(compileResult);
        }
    }

    /**
     * 测试函数：先解释执行再编译执行，并返回结果
     *
     * @param source Fluxon 源代码
     * @return 测试结果
     */
    public static TestResult run(String source) {
        return run(source, "TestScript");
    }

    /**
     * 测试函数：先解释执行再编译执行，并返回结果
     *
     * @param source    Fluxon 源代码
     * @param className 编译时使用的类名
     * @return 测试结果
     */
    public static TestResult run(String source, String className) {
        Object interpretResult;
        Object compileResult;
        long interpretTime;
        long compileTime;
        long executeTime;

        // 1. 解释执行
        System.out.println("=== 解释执行 ===");
        long startInterpret = System.currentTimeMillis();
        Environment interpretEnv = FluxonRuntime.getInstance().newEnvironment();
        interpretResult = Fluxon.eval(source, interpretEnv);
        interpretTime = System.currentTimeMillis() - startInterpret;
        System.out.println("解释执行结果: " + interpretResult);
        System.out.println("解释执行耗时: " + interpretTime + "ms");

        // 2. 编译执行
        System.out.println("\n=== 编译执行 ===");
        long startCompile = System.currentTimeMillis();
        CompileResult compileResultObj = Fluxon.compile(source, className);
        compileTime = System.currentTimeMillis() - startCompile;
        System.out.println("编译耗时: " + compileTime + "ms");

        // 3. 加载并执行编译后的类
        long startExecute = System.currentTimeMillis();
        Class<?> scriptClass = compileResultObj.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base;
        try {
            base = (RuntimeScriptBase) scriptClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Environment compileEnv = FluxonRuntime.getInstance().newEnvironment();
        compileResult = base.eval(compileEnv);
        executeTime = System.currentTimeMillis() - startExecute;
        System.out.println("编译执行结果: " + compileResult);
        System.out.println("执行耗时: " + executeTime + "ms");

        // 4. 比较结果
        TestResult result = new TestResult(interpretResult, interpretEnv, compileResult, compileEnv, interpretTime, compileTime, executeTime);
        System.out.println("\n=== 结果对比 ===");
        System.out.println("结果匹配: " + result.isMatch());
        return result;
    }

    /**
     * 静默测试：不输出日志，仅返回结果
     *
     * @param source Fluxon 源代码
     * @return 测试结果
     */
    public static TestResult runSilent(String source) {
        return runSilent(source, "TestScript");
    }

    /**
     * 静默测试：不输出日志，仅返回结果
     *
     * @param source    Fluxon 源代码
     * @param className 编译时使用的类名
     * @return 测试结果
     */
    public static TestResult runSilent(String source, String className) {
        Object interpretResult;
        Object compileResult;
        long interpretTime;
        long compileTime;
        long executeTime;

        // 1. 解释执行
        long startInterpret = System.currentTimeMillis();
        Environment interpretEnv = FluxonRuntime.getInstance().newEnvironment();
        interpretResult = Fluxon.eval(source, interpretEnv);
        interpretTime = System.currentTimeMillis() - startInterpret;

        // 2. 编译
        long startCompile = System.currentTimeMillis();
        CompileResult compileResultObj = Fluxon.compile(source, className);
        compileTime = System.currentTimeMillis() - startCompile;
        // 输出编译字节码
        try {
            compileResultObj.dump(new File("dump/TestScript.class"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 3. 执行
        long startExecute = System.currentTimeMillis();
        Class<?> scriptClass = compileResultObj.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base;
        try {
            base = (RuntimeScriptBase) scriptClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Environment compileEnv = FluxonRuntime.getInstance().newEnvironment();
        compileResult = base.eval(compileEnv);
        executeTime = System.currentTimeMillis() - startExecute;
        return new TestResult(interpretResult, interpretEnv, compileResult, compileEnv, interpretTime, compileTime, executeTime);
    }

    /**
     * 仅解释执行：只执行解释模式，不编译
     *
     * @param source Fluxon 源代码
     * @return 测试结果（仅包含解释执行结果）
     */
    public static TestResult interpret(String source) {
        long startInterpret = System.currentTimeMillis();
        Environment interpretEnv = FluxonRuntime.getInstance().newEnvironment();
        Object interpretResult = Fluxon.eval(source, interpretEnv);
        long interpretTime = System.currentTimeMillis() - startInterpret;
        return new TestResult(interpretResult, interpretEnv, null, null, interpretTime, 0, 0);
    }

    /**
     * 仅编译执行：只执行编译模式，不解释
     */
    public static TestResult compile(String source, String className) {
        // 2. 编译
        long startCompile = System.currentTimeMillis();
        CompileResult compileResultObj = Fluxon.compile(source, className);
        long compileTime = System.currentTimeMillis() - startCompile;
        // 输出编译字节码
        try {
            compileResultObj.dump(new File("dump/TestScript.class"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 3. 执行
        long startExecute = System.currentTimeMillis();
        Class<?> scriptClass = compileResultObj.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base;
        try {
            base = (RuntimeScriptBase) scriptClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Environment compileEnv = FluxonRuntime.getInstance().newEnvironment();
        Object compileResult = base.eval(compileEnv);
        long executeTime = System.currentTimeMillis() - startExecute;
        return new TestResult(null, null, compileResult, compileEnv, 0, compileTime, executeTime);
    }

    // ========== 异常测试支持 ==========

    /**
     * 测试预期抛出异常的场景
     *
     * @param source                  Fluxon 源代码
     * @param expectedMessageContains 预期异常消息包含的字符串
     */
    public static void runExpectingError(String source, String expectedMessageContains) {
        try {
            runSilent(source);
            fail("Expected exception containing: " + expectedMessageContains);
        } catch (Exception e) {
            assertTrue(e.getMessage() != null && e.getMessage().contains(expectedMessageContains), "Expected message containing '" + expectedMessageContains + "' but got: " + e.getMessage());
        }
    }

    /**
     * 测试仅解释模式下预期抛出异常的场景
     *
     * @param source                  Fluxon 源代码
     * @param expectedMessageContains 预期异常消息包含的字符串
     */
    public static void interpretExpectingError(String source, String expectedMessageContains) {
        try {
            interpret(source);
            fail("Expected exception containing: " + expectedMessageContains);
        } catch (Exception e) {
            assertTrue(e.getMessage() != null && e.getMessage().contains(expectedMessageContains), "Expected message containing '" + expectedMessageContains + "' but got: " + e.getMessage());
        }
    }

    // ========== 带环境配置的测试 ==========

    /**
     * 静默测试：支持环境预配置
     *
     * @param source   Fluxon 源代码
     * @param envSetup 环境配置函数
     * @return 测试结果
     */
    public static TestResult runSilentWithEnv(String source, Consumer<Environment> envSetup) {
        return runSilentWithEnv(source, "TestScript", envSetup);
    }

    /**
     * 静默测试：支持环境预配置
     *
     * @param source    Fluxon 源代码
     * @param className 编译时使用的类名
     * @param envSetup  环境配置函数
     * @return 测试结果
     */
    public static TestResult runSilentWithEnv(String source, String className, Consumer<Environment> envSetup) {
        Object interpretResult;
        Object compileResult;
        long interpretTime;
        long compileTime;
        long executeTime;

        // 1. 解释执行
        long startInterpret = System.currentTimeMillis();
        Environment interpretEnv = FluxonRuntime.getInstance().newEnvironment();
        envSetup.accept(interpretEnv);
        interpretResult = Fluxon.eval(source, interpretEnv);
        interpretTime = System.currentTimeMillis() - startInterpret;

        // 2. 编译
        long startCompile = System.currentTimeMillis();
        Environment compileParseEnv = FluxonRuntime.getInstance().newEnvironment();
        envSetup.accept(compileParseEnv);
        CompileResult compileResultObj = Fluxon.compile(source, className, compileParseEnv);
        compileTime = System.currentTimeMillis() - startCompile;
        // 输出编译字节码
        try {
            compileResultObj.dump(new File("dump/TestScript.class"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 3. 执行
        long startExecute = System.currentTimeMillis();
        Class<?> scriptClass = compileResultObj.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base;
        try {
            base = (RuntimeScriptBase) scriptClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Environment compileEnv = FluxonRuntime.getInstance().newEnvironment();
        envSetup.accept(compileEnv);
        compileResult = base.eval(compileEnv);
        executeTime = System.currentTimeMillis() - startExecute;
        return new TestResult(interpretResult, interpretEnv, compileResult, compileEnv, interpretTime, compileTime, executeTime);
    }

    /**
     * 仅解释执行：支持环境预配置
     *
     * @param source   Fluxon 源代码
     * @param envSetup 环境配置函数
     * @return 测试结果
     */
    public static TestResult interpretWithEnv(String source, Consumer<Environment> envSetup) {
        long startInterpret = System.currentTimeMillis();
        Environment interpretEnv = FluxonRuntime.getInstance().newEnvironment();
        envSetup.accept(interpretEnv);
        Object interpretResult = Fluxon.eval(source, interpretEnv);
        long interpretTime = System.currentTimeMillis() - startInterpret;
        return new TestResult(interpretResult, interpretEnv, null, null, interpretTime, 0, 0);
    }

    // ========== 静态断言方法 ==========

    /**
     * 断言解释结果和编译结果匹配
     *
     * @param result 测试结果
     */
    public static void assertMatch(TestResult result) {
        assertTrue(result.isMatch(), "Interpret and compile results should match. Interpret: " + result.getInterpretResult() + ", Compile: " + result.getCompileResult());
    }

    /**
     * 断言解释结果等于预期值
     *
     * @param expected 预期值
     * @param result   测试结果
     */
    public static void assertInterpretEquals(Object expected, TestResult result) {
        assertEquals(expected, result.getInterpretResult());
    }

    /**
     * 断言解释结果和编译结果都等于预期值
     *
     * @param expected 预期值
     * @param result   测试结果
     */
    public static void assertBothEqual(Object expected, TestResult result) {
        assertEquals(expected, result.getInterpretResult(), "Interpret result should equal expected");
        assertEquals(expected, result.getCompileResult(), "Compile result should equal expected");
        assertTrue(result.isMatch(), "Interpret and compile results should match");
    }

    /**
     * 断言解释结果和编译结果的字符串表示都等于预期值
     *
     * @param expected 预期的字符串表示
     * @param result   测试结果
     */
    public static void assertBothToStringEqual(String expected, TestResult result) {
        assertEquals(expected, String.valueOf(result.getInterpretResult()), "Interpret result toString should equal expected");
        assertEquals(expected, String.valueOf(result.getCompileResult()), "Compile result toString should equal expected");
    }
}
