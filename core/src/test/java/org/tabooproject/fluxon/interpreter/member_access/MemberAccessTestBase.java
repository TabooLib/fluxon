package org.tabooproject.fluxon.interpreter.member_access;

import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.parser.ParsedScript;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.type.TestObject;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.reflect.Array.*;

/**
 * 成员访问测试基类
 * 提供解释执行和编译执行的公共方法
 *
 * @author sky
 */
public abstract class MemberAccessTestBase {

    private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(0);

    /**
     * 解释执行代码（带默认 TestObject）
     */
    protected Object interpret(String source) {
        return interpret(source, new TestObject());
    }

    /**
     * 解释执行代码（自定义对象）
     */
    protected Object interpret(String source, Object obj) {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("obj", obj);
        return Fluxon.parse(ctx, env).eval(env);
    }

    /**
     * 解释执行代码（自定义环境配置）
     */
    protected Object interpret(String source, EnvironmentConfigurer configurer) {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        configurer.configure(env);
        return Fluxon.parse(ctx, env).eval(env);
    }

    /**
     * 编译执行代码（带默认 TestObject）
     */
    protected Object compile(String source) throws Exception {
        return compile(source, new TestObject(), generateClassName());
    }

    /**
     * 编译执行代码（自定义类名，带默认 TestObject）
     */
    protected Object compile(String source, String className) throws Exception {
        return compile(source, new TestObject(), className);
    }

    /**
     * 编译执行代码（自定义对象）
     */
    protected Object compile(String source, Object obj) throws Exception {
        return compile(source, obj, generateClassName());
    }

    /**
     * 编译执行代码（自定义对象和类名）
     */
    protected Object compile(String source, Object obj, String className) throws Exception {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        env.defineRootVariable("obj", obj);
        CompileResult compileResult = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base = (RuntimeScriptBase) scriptClass.newInstance();
        dumpClass(compileResult, className);
        return base.eval(env);
    }

    /**
     * 编译执行代码（自定义环境配置）
     */
    protected Object compile(String source, EnvironmentConfigurer configurer) throws Exception {
        String className = generateClassName();
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        configurer.configure(env);
        CompileResult compileResult = Fluxon.compile(env, ctx, className);
        Class<?> scriptClass = compileResult.defineClass(new FluxonClassLoader());
        RuntimeScriptBase base = (RuntimeScriptBase) scriptClass.newInstance();
        dumpClass(compileResult, className);
        return base.eval(env);
    }

    /**
     * 同时执行解释和编译模式，断言结果一致
     */
    protected Object interpretAndCompile(String source) throws Exception {
        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertResultsEqual(interpretResult, compileResult,
                "Interpret and compile results should be equal for: " + source);
        return interpretResult;
    }

    /**
     * 同时执行解释和编译模式（自定义对象）
     */
    protected Object interpretAndCompile(String source, Object obj) throws Exception {
        Object interpretResult = interpret(source, obj);
        Object compileResult = compile(source, obj);
        assertResultsEqual(interpretResult, compileResult,
                "Interpret and compile results should be equal for: " + source);
        return interpretResult;
    }

    /**
     * 解析代码（不执行）
     */
    protected ParsedScript parse(String source) {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(true);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        return Fluxon.parse(ctx, env);
    }

    /**
     * 解析代码（禁用反射访问）
     */
    protected ParsedScript parseWithoutReflection(String source) {
        CompilationContext ctx = new CompilationContext(source);
        ctx.setAllowReflectionAccess(false);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        return Fluxon.parse(ctx, env);
    }

    /**
     * 生成唯一的类名
     */
    protected String generateClassName() {
        return "Test_" + getClass().getSimpleName() + "_" + CLASS_COUNTER.incrementAndGet();
    }

    /**
     * 导出编译的类文件（用于调试）
     */
    protected void dumpClass(CompileResult result, String className) {
        try {
            File dumpDir = new File("dump");
            if (!dumpDir.exists()) {
                dumpDir.mkdirs();
            }
            result.dump(new File(dumpDir, className + ".class"));
        } catch (Exception e) {
            // 忽略 dump 错误
        }
    }

    /**
     * 比较两个结果是否相等
     */
    protected void assertResultsEqual(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null) {
            throw new AssertionError(message + " - expected: " + expected + ", actual: " + actual);
        }
        // 数组类型特殊处理
        if (expected.getClass().isArray() && actual.getClass().isArray()) {
            int expectedLength = getLength(expected);
            int actualLength = getLength(actual);
            if (expectedLength != actualLength) {
                throw new AssertionError(message + " - array lengths differ: expected " + expectedLength + ", actual " + actualLength);
            }
            for (int i = 0; i < expectedLength; i++) {
                Object expectedElement = get(expected, i);
                Object actualElement = get(actual, i);
                assertResultsEqual(expectedElement, actualElement, message + " - array element at index " + i + " differs");
            }
            return;
        }
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " - expected: " + expected + ", actual: " + actual);
        }
    }

    /**
     * 环境配置器接口
     */
    @FunctionalInterface
    protected interface EnvironmentConfigurer {
        void configure(Environment env);
    }
}
