package org.tabooproject.fluxon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.compiler.CompilationContext;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.type.TestRuntime;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 类型分析器测试
 */
public class TypeAnalyzerTest {

    @BeforeEach
    public void setup() {
        TestRuntime.registerTestFunctions();
    }

    @Test
    public void testIntLiteralAssignment() {
        String source = "_x = 10; &_x";
        CompilationContext context = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        List<ParseResult> results = Fluxon.doParse(env, context);

        System.out.println("Parse results count: " + results.size());
        for (int i = 0; i < results.size(); i++) {
            ParseResult r = results.get(i);
            System.out.println("  [" + i + "] " + r.getClass().getSimpleName() + ": " + r);
        }

        TypeAnalyzer analyzer = new TypeAnalyzer();
        analyzer.analyze(results);

        Map<Integer, Type> types = analyzer.getVariableTypes();
        System.out.println("Variable types: " + types);
        System.out.println("Type for position 0: " + analyzer.getVariableType(0));

        assertEquals(Type.I, analyzer.getVariableType(0), "Variable _x should be inferred as Type.I");
    }

    @Test
    public void testMultipleIntAssignment() {
        String source = "_a = 1; _b = 2; &_a + &_b";
        CompilationContext context = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        List<ParseResult> results = Fluxon.doParse(env, context);

        TypeAnalyzer analyzer = new TypeAnalyzer();
        analyzer.analyze(results);

        System.out.println("Variable types: " + analyzer.getVariableTypes());
        assertEquals(Type.I, analyzer.getVariableType(0), "_a should be Type.I");
        assertEquals(Type.I, analyzer.getVariableType(1), "_b should be Type.I");
    }

    @Test
    public void testDoubleAssignment() {
        String source = "_d = 1.5; &_d";
        CompilationContext context = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        List<ParseResult> results = Fluxon.doParse(env, context);

        TypeAnalyzer analyzer = new TypeAnalyzer();
        analyzer.analyze(results);

        System.out.println("Variable types: " + analyzer.getVariableTypes());
        assertEquals(Type.D, analyzer.getVariableType(0), "_d should be Type.D");
    }

    @Test
    public void testMixedTypesDegradeToObject() {
        String source = "_x = 1; _x = \"hello\"; &_x";
        CompilationContext context = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        List<ParseResult> results = Fluxon.doParse(env, context);

        TypeAnalyzer analyzer = new TypeAnalyzer();
        analyzer.analyze(results);

        System.out.println("Variable types: " + analyzer.getVariableTypes());
        assertEquals(Type.OBJECT, analyzer.getVariableType(0), "_x should degrade to OBJECT");
    }

    @Test
    public void testCompoundAssignment() {
        String source = "_x = 0; _x += 1; &_x";
        CompilationContext context = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        List<ParseResult> results = Fluxon.doParse(env, context);

        TypeAnalyzer analyzer = new TypeAnalyzer();
        analyzer.analyze(results);

        System.out.println("Variable types: " + analyzer.getVariableTypes());
        assertEquals(Type.I, analyzer.getVariableType(0), "_x with compound int assignment should stay Type.I");
    }

    @Test
    public void testBooleanAssignment() {
        String source = "_flag = true; &_flag";
        CompilationContext context = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        List<ParseResult> results = Fluxon.doParse(env, context);

        TypeAnalyzer analyzer = new TypeAnalyzer();
        analyzer.analyze(results);

        System.out.println("Variable types: " + analyzer.getVariableTypes());
        assertEquals(Type.Z, analyzer.getVariableType(0), "_flag should be Type.Z");
    }

    @Test
    public void testCompileEndToEnd() {
        // 验证编译后的字节码是否使用了基本类型存取方法
        String source = "_x = 10; _x += 5; &_x";
        CompilationContext context = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();

        // 执行完整编译流程
        CompileResult result = Fluxon.compile(env, context, "TypeTest");
        byte[] mainClass = result.getMainClass();

        // 检查字节码中是否包含 setLocalInt 方法调用
        String bytecodeStr = new String(mainClass, java.nio.charset.StandardCharsets.ISO_8859_1);
        boolean hasSetLocalInt = bytecodeStr.contains("setLocalInt");
        boolean hasSetLocalRef = bytecodeStr.contains("setLocalRef");
        boolean hasGetLocalInt = bytecodeStr.contains("getLocalInt");
        boolean hasGetLocalRef = bytecodeStr.contains("getLocalRef");

        System.out.println("Has setLocalInt: " + hasSetLocalInt);
        System.out.println("Has setLocalRef: " + hasSetLocalRef);
        System.out.println("Has getLocalInt: " + hasGetLocalInt);
        System.out.println("Has getLocalRef: " + hasGetLocalRef);

        assertTrue(hasSetLocalInt, "Compiled bytecode should use setLocalInt for int variable");
        assertFalse(hasSetLocalRef, "Compiled bytecode should NOT use setLocalRef for int variable");

        // 验证功能正确性
        org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader cl = new org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader();
        Class<?> scriptClass = result.defineClass(cl);
        try {
            org.tabooproject.fluxon.runtime.RuntimeScriptBase base = (org.tabooproject.fluxon.runtime.RuntimeScriptBase) scriptClass.newInstance();
            Environment execEnv = FluxonRuntime.getInstance().newEnvironment();
            Object execResult = base.eval(execEnv);
            System.out.println("Execution result: " + execResult);
            assertEquals(15, execResult);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testFunctionBodyTypeInference() {
        // 验证函数体内的变量类型推断
        String source = "def compute() { num1 = 10; num2 = 20; &num1 + &num2 }\ncompute()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(source);
        System.out.println("Function body result: " + result.getCompileResult());
        assertEquals(30, result.getCompileResult());
        FluxonTestUtil.assertMatch(result);

        // 验证函数类字节码使用 setLocalInt
        CompilationContext context = new CompilationContext(source);
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        CompileResult compileResult = Fluxon.compile(env, context, "FuncTypeTest");
        // 函数类是 innerClasses 的第一个
        List<byte[]> innerClasses = compileResult.getInnerClasses();
        assertFalse(innerClasses.isEmpty(), "Should have function inner class");
        byte[] funcClass = innerClasses.get(0);
        String funcBytecodeStr = new String(funcClass, java.nio.charset.StandardCharsets.ISO_8859_1);
        boolean hasSetLocalInt = funcBytecodeStr.contains("setLocalInt");
        boolean hasSetLocalRef = funcBytecodeStr.contains("setLocalRef");
        System.out.println("Function class has setLocalInt: " + hasSetLocalInt);
        System.out.println("Function class has setLocalRef: " + hasSetLocalRef);
        assertTrue(hasSetLocalInt, "Function bytecode should use setLocalInt for int variables");
        assertFalse(hasSetLocalRef, "Function bytecode should NOT use setLocalRef for int variables");
    }
}
