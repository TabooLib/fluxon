package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 匿名类表达式测试
 *
 * @author sky
 */
public class AnonymousClassTest {

    @Test
    public void testImplementCallable() {
        String script =
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return 42\n" +
                "    }\n" +
                "}\n" +
                "&callable";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestCallable");
        assertNotNull(result.getCompileResult());
    }

    @Test
    public void testCallableMethodCall() {
        String script =
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return \"hello\"\n" +
                "    }\n" +
                "}\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestCallableCall");
        assertEquals("hello", result.getCompileResult());
    }

    @Test
    public void testCallableReturnInt() {
        String script =
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return 100\n" +
                "    }\n" +
                "}\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestCallableInt");
        assertEquals(100, result.getCompileResult());
    }

    @Test
    public void testComparatorSimple() {
        String script =
                "cmp = impl : java.util.Comparator {\n" +
                "    override compare(a, b) {\n" +
                "        return 1\n" +
                "    }\n" +
                "}\n" +
                "&cmp.compare(\"a\", \"b\")";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestCmpSimple");
        assertEquals(1, result.getCompileResult());
    }

    // ========== 构造参数测试 ==========

    @Test
    public void testCtorWithInt() {
        // ArrayList(int) - 单个 int 参数
        String script =
                "list = impl : java.util.ArrayList(100) {\n" +
                "}\n" +
                "&list.size()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestCtorInt");
        assertEquals(0, result.getCompileResult());
    }

    @Test
    public void testCtorWithString() {
        // TestPerson(String) - 单个 String 参数
        String script =
                "person = impl : org.tabooproject.fluxon.interpreter.helper.TestPerson(\"Alice\") {\n" +
                "}\n" +
                "&person.getName()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestCtorString");
        assertEquals("Alice", result.getCompileResult());
    }

    @Test
    public void testCtorWithTwoArgs() {
        // TestPerson(String, int) - 两个参数
        String script =
                "person = impl : org.tabooproject.fluxon.interpreter.helper.TestPerson(\"Bob\", 25) {\n" +
                "}\n" +
                "&person.getAge()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestCtorTwoArgs");
        assertEquals(25, result.getCompileResult());
    }

    @Test
    public void testCtorWithMethod() {
        // TestPerson 并调用其方法
        String script =
                "person = impl : org.tabooproject.fluxon.interpreter.helper.TestPerson(\"World\") {\n" +
                "}\n" +
                "&person.greet()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestCtorMethod");
        assertEquals("Hello, World", result.getCompileResult());
    }

    @Test
    public void testAbstractClassWithCtor() {
        // TestProcessor(String) - 抽象类带构造参数
        String script =
                "processor = impl : org.tabooproject.fluxon.interpreter.helper.TestProcessor(\"LOG\") {\n" +
                "    override process(input) {\n" +
                "        return input\n" +
                "    }\n" +
                "}\n" +
                "&processor.getPrefix()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestAbstractCtor");
        assertEquals("LOG", result.getCompileResult());
    }

    @Test
    public void testAbstractClassRun() {
        // TestProcessor - 调用父类方法（使用覆写的方法）
        String script =
                "processor = impl : org.tabooproject.fluxon.interpreter.helper.TestProcessor(\"INFO\") {\n" +
                "    override process(input) {\n" +
                "        return &input.toUpperCase()\n" +
                "    }\n" +
                "}\n" +
                "&processor.run(\"hello\")";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestAbstractRun");
        assertEquals("INFO: HELLO", result.getCompileResult());
    }

    @Test
    public void testCtorWithExpression() {
        // 构造参数使用表达式
        String script =
                "name = \"Test\"\n" +
                "person = impl : org.tabooproject.fluxon.interpreter.helper.TestPerson(&name + \"User\") {\n" +
                "}\n" +
                "&person.getName()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestCtorExpr");
        assertEquals("TestUser", result.getCompileResult());
    }

    // ========== 外部访问测试 ==========

    @Test
    public void testAccessOuterVariable() {
        String script =
                "outerVar = \"OUTER\"\n" +
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return &outerVar\n" +
                "    }\n" +
                "}\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestAccessVar");
        assertEquals("OUTER", result.getCompileResult());
    }

    @Test
    public void testAccessOuterFunctionNoArgs() {
        String script =
                "def getMsg() = \"Hello\"\n" +
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return getMsg()\n" +
                "    }\n" +
                "}\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestAccessFunc");
        assertEquals("Hello", result.getCompileResult());
    }

    @Test
    public void testAccessOuterFunctionWithArgs() {
        String script =
                "def greet(x) = \"Hello \" + &x\n" +
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return greet(\"World\")\n" +
                "    }\n" +
                "}\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestAccessFuncArgs");
        assertEquals("Hello World", result.getCompileResult());
    }

    // ========== 多接口测试 ==========

    @Test
    public void testMultipleInterfaces() {
        // 同时实现 Runnable 和 Callable
        String script =
                "result = \"\"\n" +
                "obj = impl : java.lang.Runnable, java.util.concurrent.Callable {\n" +
                "    override run() {\n" +
                "        result = \"ran\"\n" +
                "    }\n" +
                "    override call() {\n" +
                "        return \"called\"\n" +
                "    }\n" +
                "}\n" +
                "&obj.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestMultiInterface");
        assertEquals("called", result.getCompileResult());
    }

    @Test
    public void testMultipleInterfacesCallable() {
        // 验证实现了 Callable 接口
        String script =
                "obj = impl : java.lang.Runnable, java.util.concurrent.Callable {\n" +
                "    override run() {\n" +
                "    }\n" +
                "    override call() {\n" +
                "        return 123\n" +
                "    }\n" +
                "}\n" +
                "&obj.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestMultiCallable");
        assertEquals(123, result.getCompileResult());
    }

    // ========== 多方法覆写测试 ==========

    @Test
    public void testMultipleMethods() {
        String script =
                "handler = impl : org.tabooproject.fluxon.interpreter.helper.TestHandler() {\n" +
                "    override onStart() {\n" +
                "        return \"started\"\n" +
                "    }\n" +
                "    override onStop() {\n" +
                "        return \"stopped\"\n" +
                "    }\n" +
                "    override onData(data) {\n" +
                "        return \"data:\" + &data\n" +
                "    }\n" +
                "}\n" +
                "&handler.onStart() + \"|\" + &handler.onStop() + \"|\" + &handler.onData(\"test\")";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestMultiMethods");
        assertEquals("started|stopped|data:test", result.getCompileResult());
    }

    // ========== 复杂逻辑测试 ==========

    @Test
    public void testMethodWithLocalVar() {
        // 方法内定义局部变量
        String script =
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        x = 10\n" +
                "        return &x * 2\n" +
                "    }\n" +
                "}\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestLocalVar");
        assertEquals(20, result.getCompileResult());
    }

    @Test
    public void testMethodWithIf() {
        // 方法内 if 条件判断
        String script =
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        x = 10\n" +
                "        if (&x > 5) {\n" +
                "            return \"big\"\n" +
                "        }\n" +
                "        return \"small\"\n" +
                "    }\n" +
                "}\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestMethodIf");
        assertEquals("big", result.getCompileResult());
    }

    @Test
    public void testMethodWithWhile() {
        // 方法内 while 循环
        String script =
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        sum = 0\n" +
                "        i = 1\n" +
                "        while (&i <= 5) {\n" +
                "            sum = &sum + &i\n" +
                "            i = &i + 1\n" +
                "        }\n" +
                "        return &sum\n" +
                "    }\n" +
                "}\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestMethodWhile");
        assertEquals(15, result.getCompileResult());
    }

    @Test
    public void testMethodWithMultipleLocalVars() {
        // 方法内多个局部变量
        String script =
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        a = \"Hello\"\n" +
                "        b = \" \"\n" +
                "        c = \"World\"\n" +
                "        return &a + &b + &c\n" +
                "    }\n" +
                "}\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestMultiLocalVars");
        assertEquals("Hello World", result.getCompileResult());
    }

    @Test
    public void testMethodParamWithLocalVar() {
        // 方法参数与局部变量混合使用
        String script =
                "cmp = impl : java.util.Comparator {\n" +
                "    override compare(a, b) {\n" +
                "        lenA = &a.toString().length()\n" +
                "        lenB = &b.toString().length()\n" +
                "        return &lenA - &lenB\n" +
                "    }\n" +
                "}\n" +
                "&cmp.compare(\"short\", \"verylongstring\")";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestParamWithLocal");
        assertTrue((int) result.getCompileResult() < 0);
    }

    @Test
    public void testMethodParamModify() {
        String script =
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return transform(\"hello\")\n" +
                "    }\n" +
                "}\n" +
                "def transform(s) = &s.toUpperCase()\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestParamModify");
        assertEquals("HELLO", result.getCompileResult());
    }

    @Test
    public void testMethodParamCompare() {
        // 直接在方法内使用参数进行比较
        String script =
                "cmp = impl : java.util.Comparator {\n" +
                "    override compare(a, b) {\n" +
                "        return &a.toString().length() - &b.toString().length()\n" +
                "    }\n" +
                "}\n" +
                "&cmp.compare(\"short\", \"verylongstring\")";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestParamCompare");
        assertTrue((int) result.getCompileResult() < 0);
    }

    // ========== 嵌套与边界测试 ==========

    @Test
    public void testMultipleAnonymousClasses() {
        String script =
                "c1 = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return 1\n" +
                "    }\n" +
                "}\n" +
                "c2 = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return 2\n" +
                "    }\n" +
                "}\n" +
                "c3 = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return 3\n" +
                "    }\n" +
                "}\n" +
                "&c1.call() + &c2.call() + &c3.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestMultiAnon");
        assertEquals(6, result.getCompileResult());
    }

    @Test
    public void testComparatorWithList() {
        // 使用 Comparator 比较元素
        String script =
                "list = new java.util.ArrayList()\n" +
                "&list.add(\"banana\")\n" +
                "&list.add(\"apple\")\n" +
                "cmp = impl : java.util.Comparator {\n" +
                "    override compare(a, b) {\n" +
                "        return &a.compareTo(&b)\n" +
                "    }\n" +
                "}\n" +
                "&cmp.compare(&list.get(0), &list.get(1))";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestCmpWithList");
        // "banana".compareTo("apple") > 0
        assertTrue((int) result.getCompileResult() > 0);
    }

    @Test
    public void testAccessMultipleOuterVars() {
        String script =
                "prefix = \"[\"\n" +
                "suffix = \"]\"\n" +
                "content = \"data\"\n" +
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return &prefix + &content + &suffix\n" +
                "    }\n" +
                "}\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestMultiOuterVars");
        assertEquals("[data]", result.getCompileResult());
    }

    @Test
    public void testReturnBoolean() {
        String script =
                "pred = impl : java.util.function.Predicate {\n" +
                "    override test(t) {\n" +
                "        return &t.toString().length() > 3\n" +
                "    }\n" +
                "}\n" +
                "&pred.test(\"hello\")";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestReturnBool");
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testReturnNull() {
        String script =
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return null\n" +
                "    }\n" +
                "}\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestReturnNull");
        assertNull(result.getCompileResult());
    }

    @Test
    public void testEmptyMethodBody() {
        String script =
                "runnable = impl : java.lang.Runnable {\n" +
                "    override run() {\n" +
                "    }\n" +
                "}\n" +
                "&runnable.run()\n" +
                "\"done\"";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestEmptyBody");
        assertEquals("done", result.getCompileResult());
    }

    @Test
    public void testChainedMethodCalls() {
        String script =
                "callable = impl : java.util.concurrent.Callable {\n" +
                "    override call() {\n" +
                "        return \"  hello world  \".trim().toUpperCase()\n" +
                "    }\n" +
                "}\n" +
                "&callable.call()";
        FluxonTestUtil.TestResult result = FluxonTestUtil.compile(script, "TestChained");
        assertEquals("HELLO WORLD", result.getCompileResult());
    }
}
