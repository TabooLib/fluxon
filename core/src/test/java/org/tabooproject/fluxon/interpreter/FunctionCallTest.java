package org.tabooproject.fluxon.interpreter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.tabooproject.fluxon.Fluxon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.runtime.error.FunctionNotFoundError;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 函数调用测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class FunctionCallTest {

    @Test
    public void testZeroParamFunctionWithOperator() {
        // now() 会读取真实时钟，两次调用之间允许出现毫秒级推进。
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "start = now(); " +
                        "end = now() + 1000; " +
                        "diff = &end - &start; " +
                        "&diff");
        assertTrue(((Number) result.getInterpretResult()).longValue() >= 1000L);
        assertTrue(((Number) result.getCompileResult()).longValue() >= 1000L);
    }

    @Test
    public void testFunctionWithExpressionArg() {
        // 测试函数参数是表达式
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 5; " +
                        "b = 3; " +
                        "&a - &b");
        assertEquals(2, result.getInterpretResult());
        assertEquals(2, result.getCompileResult());
    }

    @Test
    public void testSimpleFunction() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def inc(x) = &x + 1; " +
                        "inc(5)");
        assertEquals(6, result.getInterpretResult());
        assertEquals(6, result.getCompileResult());
    }

    @Test
    public void testFunctionWithMultipleParams() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def add(x, y) = &x + &y; " +
                        "add(3, 4)");
        assertEquals(7, result.getInterpretResult());
        assertEquals(7, result.getCompileResult());
    }

    @Test
    public void testMultipleArgsWithExpressions() {
        // 测试多个参数，其中包含表达式
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def myMax(x, y) = if &x > &y then &x else &y; " +
                        "a = 10; " +
                        "b = 5; " +
                        "myMax(&a + &b, &a * &b)");
        // myMax(15, 50) = 50
        assertEquals(50, result.getInterpretResult());
        assertEquals(50, result.getCompileResult());
    }

    @Test
    public void testChainedCalls() {
        // 测试链式调用
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def inc(x) = &x + 1; " +
                        "def twice(x) = &x * 2; " +
                        "twice(inc(5))");
        // twice(inc(5)) = twice(6) = 12
        assertEquals(12, result.getInterpretResult());
        assertEquals(12, result.getCompileResult());
    }

    @Test
    public void testNestedFunctionCalls() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def add(x, y) = &x + &y; " +
                        "def mul(x, y) = &x * &y; " +
                        "add(mul(2, 3), mul(4, 5))");
        // add(2*3, 4*5) = add(6, 20) = 26
        assertEquals(26, result.getInterpretResult());
        assertEquals(26, result.getCompileResult());
    }

    @Test
    public void testFunctionWithReturnValue() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def getValue = 42; " +
                        "getValue()");
        assertEquals(42, result.getInterpretResult());
        assertEquals(42, result.getCompileResult());
    }

    @Test
    public void testFunctionWithConditionalReturn() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent(
                "def myAbs(x) = if &x < 0 then -&x else &x; " +
                        "myAbs(-5)");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());

        result = FluxonTestUtil.runSilent(
                "def myAbs(x) = if &x < 0 then -&x else &x; " +
                        "myAbs(5)");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());
    }

    @Test
    public void testParameterBinding() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def test(a, b, c) = &a + &b + &c; " +
                        "test(1, 2, 3)");
        assertEquals(6, result.getInterpretResult());
        assertEquals(6, result.getCompileResult());
    }

    @Test
    public void testParameterWithSameName() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = 100\n" +
                        "def func(x) = &x * 2\n" +
                        "result = func(5)\n" +
                        "[&x, &result]");
        assertEquals("[100, 10]", result.getInterpretResult().toString());
        assertEquals("[100, 10]", result.getCompileResult().toString());
    }

    @Test
    public void testFunctionAccessOuterVariable() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "outer = 10; " +
                        "def func(x) = &x + &outer; " +
                        "func(5)");
        assertEquals(15, result.getInterpretResult());
        assertEquals(15, result.getCompileResult());
    }

    @Test
    public void testFunctionWithLocalVariable() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def func(x) = { y = &x * 2; &y + 1 }; " +
                        "func(5)");
        assertEquals(11, result.getInterpretResult());
        assertEquals(11, result.getCompileResult());
    }

    @Test
    public void testFunctionInExpression() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def inc(x) = &x + 1; " +
                        "inc(5) * inc(3)");
        // (5+1) * (3+1) = 6 * 4 = 24
        assertEquals(24, result.getInterpretResult());
        assertEquals(24, result.getCompileResult());
    }

    @Test
    public void testFunctionInConditional() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def isEven(x) = &x % 2 == 0; " +
                        "if isEven(4) then 'yes' else 'no'");
        assertEquals("yes", result.getInterpretResult());
        assertEquals("yes", result.getCompileResult());
    }

    @Test
    public void testFunctionInLoop() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def square(x) = &x * &x; " +
                        "result = []; " +
                        "for i in 1..5 { &result += square(&i) }; " +
                        "&result");
        assertEquals("[1, 4, 9, 16, 25]", result.getInterpretResult().toString());
        assertEquals("[1, 4, 9, 16, 25]", result.getCompileResult().toString());
    }

    @Test
    public void testExpressionFunctionUsesDirectCallBytecode() {
        CompileResult result = Fluxon.compile(
                "def inc(x) = &x + 1\n" +
                        "inc(5)",
                "UserDirectShapeTest"
        );
        assertTrue(hasMethodInvocation(result, "callDirect"));
    }

    @Test
    public void testExpressionFunctionDirectCallWithBooleanParameter() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def pick(flag: boolean) = &flag ? 7 : 3\n" +
                        "pick(true)");
        FluxonTestUtil.assertBothEqual(7, result);
    }

    @Test
    public void testExpressionFunctionDirectCallKeepsPrimitiveReturn() {
        String source = "def inc(x: int) = &x + 1\n" +
                "inc(5) + 2";
        FluxonTestUtil.TestResult runResult = FluxonTestUtil.runSilent(source);
        FluxonTestUtil.assertBothEqual(8, runResult);
        CompileResult result = Fluxon.compile(
                source,
                "UserDirectPrimitiveReturnTest"
        );
        assertTrue(hasMethodInvocation(result, "callDirect"));
        assertFalse(hasMethodInvocation(result, "add"));
    }

    @Test
    public void testExpressionFunctionDirectCallUsesPrimitiveParameterDescriptor() {
        CompileResult result = Fluxon.compile(
                "def inc(x: int) = &x + 1\n" +
                        "inc(5)",
                "UserDirectPrimitiveParameterTest"
        );
        assertTrue(hasMethodInvocation(result, "callDirect", "(Lorg/tabooproject/fluxon/runtime/Environment;I)Ljava/lang/Object;"));
    }

    @Test
    public void testExpressionFunctionDirectCallUsesWidePrimitiveParameterSlots() {
        String source = "def mix(a: long, b: double) = &a + &b\n" +
                "mix(2, 0.5)";
        FluxonTestUtil.TestResult runResult = FluxonTestUtil.runSilent(source);
        FluxonTestUtil.assertBothEqual(2.5, runResult);
        CompileResult result = Fluxon.compile(source, "UserDirectWidePrimitiveParameterTest");
        assertTrue(hasMethodInvocation(result, "callDirect", "(Lorg/tabooproject/fluxon/runtime/Environment;JD)Ljava/lang/Object;"));
    }

    @Test
    public void testPrintUsesDirectOutputBytecode() {
        CompileResult result = Fluxon.compile("print('ok')", "PrintDirectOutputShapeTest");
        assertTrue(hasMethodInvocation(result, "println"));
        assertFalse(hasMethodInvocation(result, "prepareCall"));
    }

    @Test
    public void testThrowUsesDirectBytecode() {
        FluxonTestUtil.TestResult runResult = FluxonTestUtil.runSilent("try throw('error') catch 'ok'");
        FluxonTestUtil.assertBothEqual("ok", runResult);
        CompileResult result = Fluxon.compile("try throw('error') catch 'ok'", "ThrowDirectShapeTest");
        assertFalse(hasMethodInvocation(result, "prepareCall"));
    }

    @Test
    public void testFunctionWithContextCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def getName = 'hello'; " +
                        "getName()::uppercase()");
        assertEquals("HELLO", result.getInterpretResult());
        assertEquals("HELLO", result.getCompileResult());
    }

    @Test
    public void testFunctionReturnsList() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def getList = [1, 2, 3]; " +
                        "getList()");
        assertEquals("[1, 2, 3]", result.getInterpretResult().toString());
        assertEquals("[1, 2, 3]", result.getCompileResult().toString());
    }

    @Test
    public void testFunctionReturnsMap() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def getMap = [a: 1, b: 2]; " +
                        "map = getMap(); " +
                        "&map['a']");
        assertEquals(1, result.getInterpretResult());
        assertEquals(1, result.getCompileResult());
    }

    @Test
    public void testSimpleRecursion() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def fib(n) = if &n <= 1 then &n else { &n + fib(&n - 1); }; " +
                        "fib(6)");
        assertEquals(21, result.getInterpretResult());
        assertEquals(21, result.getCompileResult());
    }

    @Test
    public void testFactorial() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def fact(n) = if &n <= 1 then 1 else &n * fact(&n - 1); " +
                        "fact(5)");
        assertEquals(120, result.getInterpretResult());
        assertEquals(120, result.getCompileResult());
    }

    @Test
    public void testForwardReference() {
        // 测试函数可以在定义前调用（前向引用）
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = foo(10); " +
                        "def foo(x) = &x * 2; " +
                        "&result");
        assertEquals(20, result.getInterpretResult());
        assertEquals(20, result.getCompileResult());
    }

    @Test
    public void testForwardReferenceWithMultipleFunctions() {
        // 测试多个函数的前向引用
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "result = add(mul(2, 3), mul(4, 5)); " +
                        "def add(x, y) = &x + &y; " +
                        "def mul(x, y) = &x * &y; " +
                        "&result");
        // add(mul(2, 3), mul(4, 5)) = add(6, 20) = 26
        assertEquals(26, result.getInterpretResult());
        assertEquals(26, result.getCompileResult());
    }

    @Test
    public void testForwardReferenceInExpression() {
        // 测试表达式中的前向引用
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 5; " +
                        "b = d(&a) + 3; " +
                        "def d(x) = &x * 2; " +
                        "&b");
        // d(5) + 3 = 10 + 3 = 13
        assertEquals(13, result.getInterpretResult());
        assertEquals(13, result.getCompileResult());
    }

    @Test
    public void testMixedForwardAndBackwardReference() {
        // 测试混合的前向和后向引用
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def inc(x) = &x + 1; " +
                        "a = inc(5); " +
                        "b = dec(10); " +
                        "def dec(x) = &x - 1; " +
                        "[&a, &b]");
        assertEquals("[6, 9]", result.getInterpretResult().toString());
        assertEquals("[6, 9]", result.getCompileResult().toString());
    }

    @Test
    public void testFunctionWithZeroArguments() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def getConstant = 42; " +
                        "getConstant()");
        assertEquals(42, result.getInterpretResult());
        assertEquals(42, result.getCompileResult());
    }

    @Test
    public void testFunctionWithNullReturn() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def returnNull = null; " +
                        "returnNull()");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }

    @Test
    public void testFunctionWithBooleanReturn() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent(
                "def isTrue = true; " +
                        "isTrue()");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());

        result = FluxonTestUtil.runSilent(
                "def isFalse = false; " +
                        "isFalse()");
        assertEquals(false, result.getInterpretResult());
        assertEquals(false, result.getCompileResult());
    }

    @Test
    public void testFunctionNotFoundError() {
        try {
            FluxonTestUtil.runSilent(
                    "&PI::replace(\"a\", \"b\")"
            );
            throw new RuntimeException("Should throw FunctionNotFoundError");
        } catch (FunctionNotFoundError e) {
            assertTrue(e.getMessage().contains("Double::replace(args=2)"));
        }
    }

    @Test
    public void testFunctionWithNestedLoopsAndListDeclaration() {
        // 测试函数内声明列表变量并使用嵌套循环
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def buildList(n) = {\n" +
                        "  result = []\n" +
                        "  for y in 0..&n {\n" +
                        "    for x in 0..&n {\n" +
                        "      &result::add(&x + &y * 10)\n" +
                        "    }\n" +
                        "  }\n" +
                        "  &result\n" +
                        "}\n" +
                        "buildList(2)");
        assertEquals("[0, 1, 2, 10, 11, 12, 20, 21, 22]", result.getInterpretResult().toString());
        assertEquals("[0, 1, 2, 10, 11, 12, 20, 21, 22]", result.getCompileResult().toString());
    }

    // 返回包装类型（Integer）的 DirectBinding 函数，编译模式下不应产生 VerifyError
    @Test
    public void testBoxedReturnDirectBinding_intOrNull() {
        // intOrNull 返回 Integer（可能为 null），Java 方法签名返回 Ljava/lang/Integer;
        // TYPE_MAP 将 Integer.class 映射到 Type.I（primitive），
        // DirectBinding 必须正确处理 JVM 栈上的 boxed 类型
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("intOrNull('123')");
        FluxonTestUtil.assertBothEqual(123, result);
    }

    @Test
    public void testBoxedReturnDirectBinding_intOrNullReturnsNull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("intOrNull('abc')");
        FluxonTestUtil.assertBothEqual(null, result);
    }

    @Test
    public void testBoxedReturnDirectBinding_nullComparison() {
        // intOrNull 返回值与 null 比较
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("intOrNull('123') != null");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testBoxedReturnDirectBinding_nullComparisonFalse() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("intOrNull('abc') != null");
        FluxonTestUtil.assertBothEqual(false, result);
    }

    @Test
    public void testBoxedReturnDirectBinding_logicalExpression() {
        // 模拟 isEndWithNumber 的逻辑
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "str = 'item_1'\n" +
                "last = &str::split('_')::last()\n" +
                "&last != null && intOrNull(&last) != null");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testBoxedReturnDirectBinding_logicalExpressionFalse() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "str = 'item_abc'\n" +
                "last = &str::split('_')::last()\n" +
                "&last != null && intOrNull(&last) != null");
        FluxonTestUtil.assertBothEqual(false, result);
    }

    @Test
    public void testBoxedReturnDirectBinding_longOrNull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("longOrNull('456')");
        FluxonTestUtil.assertBothEqual(456L, result);
    }

    @Test
    public void testBoxedReturnDirectBinding_doubleOrNull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("doubleOrNull('3.14')");
        FluxonTestUtil.assertBothEqual(3.14, result);
    }

    @Test
    public void testBoxedReturnDirectBinding_floatOrNull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("floatOrNull('2.5')");
        FluxonTestUtil.assertBothEqual(2.5f, result);
    }

    @Test
    public void testDirectBinding_stringParamExtension_split() {
        // split(String, String) — descriptor 期望 String 但栈上是 Object
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "'a,b,c'::split(',')::size()");
        FluxonTestUtil.assertBothEqual(3, result);
    }

    @Test
    public void testDirectBinding_stringParamExtension_replace() {
        // replace(String, String, String) — 两个 String 参数
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "'hello world'::replace('world', 'fluxon')");
        FluxonTestUtil.assertBothEqual("hello fluxon", result);
    }

    @Test
    public void testDirectBinding_stringParamExtension_startsWith() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "'hello'::startsWith('hel')");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testDirectBinding_stringParamExtension_contains() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "'hello world'::contains('world')");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testDirectBinding_stringParamExtension_indexOf() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "'hello'::indexOf('ll')");
        FluxonTestUtil.assertBothEqual(2, result);
    }

    @Test
    public void testDirectBinding_stringParamVariableArg() {
        // 参数来自变量（编译期类型为 OBJECT）
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "sep = ','\n" +
                "'a,b,c'::split(&sep)::size()");
        FluxonTestUtil.assertBothEqual(3, result);
    }

    private static boolean hasMethodInvocation(CompileResult result, String method) {
        return hasMethodInvocation(result, method, null);
    }

    private static boolean hasMethodInvocation(CompileResult result, String method, String expectedDescriptor) {
        boolean[] matched = {false};
        ClassReader reader = new ClassReader(result.getMainClass());
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, visitor) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String actualName, String actualDescriptor, boolean isInterface) {
                        if (method.equals(actualName) && (expectedDescriptor == null || expectedDescriptor.equals(actualDescriptor))) {
                            matched[0] = true;
                        }
                        super.visitMethodInsn(opcode, owner, actualName, actualDescriptor, isInterface);
                    }
                };
            }
        }, 0);
        return matched[0];
    }
}
