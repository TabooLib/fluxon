package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.FunctionSignature;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.java.Export;
import org.tabooproject.fluxon.runtime.java.Optional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 嵌套上下文调用 Bug 复现测试
 *
 * Bug 描述：当函数的最后一个参数是上下文调用（:: 操作符）时，
 * 编译模式下外层函数返回最后一个参数的值，而非外层函数本身的返回值。
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class NestedContextCallBugTest {

    // Scenario A: 多参数，最后一个参数是上下文调用（同一目标）
    // "hello world"::replace("world", "hello"::substring(0, 2))
    // Expected: "hello he"
    // Bug: "he" (返回最后一个参数的值)
    @Test
    public void testScenarioA_lastArgContextCallSameTarget() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"hello world\"::replace(\"world\", \"hello\"::substring(0, 2))"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("hello he", result.getCompileResult(),
                "Compiled result should be 'hello he', got: " + result.getCompileResult());
    }

    // Scenario B: 多参数，最后一个参数是上下文调用（不同目标）
    // "hello world"::replace("world", "test"::substring(0, 2))
    // Expected: "hello te"
    @Test
    public void testScenarioB_lastArgContextCallDifferentTarget() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"hello world\"::replace(\"world\", \"test\"::substring(0, 2))"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("hello te", result.getCompileResult(),
                "Compiled result should be 'hello te', got: " + result.getCompileResult());
    }

    // Scenario C: 扩展函数 + 上下文调用参数
    // "hello world"::substring(0, "world"::length())
    // Expected: "hello" (substring(0, 5))
    // Bug: 5 (返回最后一个参数的值)
    @Test
    public void testScenarioC_extensionFuncWithContextCallArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"hello world\"::substring(0, \"world\"::length())"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("hello", result.getCompileResult(),
                "Compiled result should be 'hello', got: " + result.getCompileResult());
    }

    // Scenario D: 多个上下文调用参数
    // "hello world"::substring("he"::length(), "hello world"::length())
    // Expected: "llo world" (substring(2, 11))
    @Test
    public void testScenarioD_multipleContextCallArgs() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"hello world\"::substring(\"he\"::length(), \"hello world\"::length())"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("llo world", result.getCompileResult(),
                "Compiled result should be 'llo world', got: " + result.getCompileResult());
    }

    // Scenario E: 用户定义函数 + 上下文调用作为最后一个参数
    // def myFunc(a, b, c) = &a + &b + &c
    // myFunc(1, 2, [10, 20, 30]::size())
    // Expected: 6 (1 + 2 + 3)
    // Bug: 3 (返回最后一个参数的值)
    @Test
    public void testScenarioE_userDefinedFuncWithContextCallLastArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def myFunc(a, b, c) = &a + &b + &c; myFunc(1, 2, [10, 20, 30]::size())"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(6, result.getCompileResult(),
                "Compiled result should be 6, got: " + result.getCompileResult());
    }

    // Scenario F: 超过 2 个参数，验证返回值不是最后一个参数
    // def myFunc(a, b, c, d, e) = &a
    // myFunc("x", "y", "z", "w", [1,2,3]::size())
    // Expected: "x"
    // Bug: 3
    @Test
    public void testScenarioF_moreThanTwoArgs() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def myFunc(a, b, c, d, e) = &a; myFunc(\"x\", \"y\", \"z\", \"w\", [1,2,3]::size())"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("x", result.getCompileResult(),
                "Compiled result should be 'x', got: " + result.getCompileResult());
    }

    // Scenario G: 只有一个上下文调用参数（应该已经可以工作）
    // [1, 2, 3]::get([10, 20, 30]::indexOf(20))
    // Expected: 2 (get(1))
    @Test
    public void testScenarioG_singleContextCallArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "[1, 2, 3]::get([10, 20, 30]::indexOf(20))"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(2, result.getCompileResult(),
                "Compiled result should be 2, got: " + result.getCompileResult());
    }

    // Scenario H: 上下文调用作为中间参数（非最后一个）
    // "hello world"::replace("hello"::substring(0, 2), "XX")
    // Expected: "XXllo world" (replace "he" with "XX")
    @Test
    public void testScenarioH_contextCallAsMiddleArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"hello world\"::replace(\"hello\"::substring(0, 2), \"XX\")"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("XXllo world", result.getCompileResult(),
                "Compiled result should be 'XXllo world', got: " + result.getCompileResult());
    }

    // Scenario I: 通过变量引用进行嵌套上下文调用
    // list = [10, 20, 30]; "hello world"::substring(0, &list::size())
    // Expected: "hel" (substring(0, 3))
    @Test
    public void testScenarioI_refVarContextCallAsLastArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "list = [10, 20, 30]; \"hello world\"::substring(0, &list::size())"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("hel", result.getCompileResult(),
                "Compiled result should be 'hel', got: " + result.getCompileResult());
    }

    // Scenario J: 使用 import 的函数模块 + 上下文调用作为最后参数
    // import 'fs:time' 场景，模拟更接近用户报告的 domain 函数调用
    @Test
    public void testScenarioJ_importedDomainWithContextCallLastArg() {
        // time::formatTimestamp 接收一个参数，用 [1755611940830L]::get(0) 作为上下文调用参数
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:time'; time::formatTimestamp([1755611940830L]::get(0))"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("2025-08-19 21:59:00", result.getCompileResult(),
                "Compiled result should be '2025-08-19 21:59:00', got: " + result.getCompileResult());
    }

    // Scenario K: 嵌套上下文调用 - 外层结果被赋值并使用
    // result = "hello world"::replace("world", "hello"::substring(0, 2)); &result
    // Expected: "hello he"
    @Test
    public void testScenarioK_assignedNestedContextCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "r = \"hello world\"::replace(\"world\", \"hello\"::substring(0, 2)); &r"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("hello he", result.getCompileResult(),
                "Compiled result should be 'hello he', got: " + result.getCompileResult());
    }

    // Scenario L: 上下文调用结果用于后续运算，验证不会被污染
    // "hello world"::replace("world", "hello"::substring(0, 2)) + "!"
    // Expected: "hello he!"
    @Test
    public void testScenarioL_contextCallResultUsedInExpression() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"hello world\"::replace(\"world\", \"hello\"::substring(0, 2)) + \"!\""
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("hello he!", result.getCompileResult(),
                "Compiled result should be 'hello he!', got: " + result.getCompileResult());
    }

    // Scenario M: 变量目标的上下文调用，最后参数也是变量目标的上下文调用
    // a = "hello world"; b = "hello"; a::replace("world", b::substring(0, 2))
    // Expected: "hello he"
    @Test
    public void testScenarioM_varTargetContextCallWithVarContextCallLastArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = \"hello world\"; b = \"hello\"; &a::replace(\"world\", &b::substring(0, 2))"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("hello he", result.getCompileResult(),
                "Compiled result should be 'hello he', got: " + result.getCompileResult());
    }

    // Scenario N: 三层嵌套上下文调用
    // "hello world"::substring(0, "world"::substring(0, "ab"::length())::length())
    // "ab"::length() = 2, "world"::substring(0, 2) = "wo", "wo"::length() = 2
    // "hello world"::substring(0, 2) = "he"
    @Test
    public void testScenarioN_tripleNestedContextCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"hello world\"::substring(0, \"world\"::substring(0, \"ab\"::length())::length())"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("he", result.getCompileResult(),
                "Compiled result should be 'he', got: " + result.getCompileResult());
    }

    // Scenario O: 全局上下文前缀 g:: + 上下文调用参数
    // 使用 g::random 模拟全局函数调用中的上下文参数
    @Test
    public void testScenarioO_globalContextWithContextCallArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "[1, 2, 3]::get(g::random([1]::get(0)))"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario P: 用户定义函数，返回值来自计算而非最后一个参数
    // 验证函数内部运算正确，不返回最后一个参数值
    @Test
    public void testScenarioP_userFuncReturnCalculation() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def concat(a, b, c) = &a + &b; concat(\"hello\", \" world\", [1,2,3]::size())"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("hello world", result.getCompileResult(),
                "Compiled result should be 'hello world' (ignoring last arg), got: " + result.getCompileResult());
    }

    // Scenario Q: 链式上下文调用后，嵌套上下文调用作为参数
    // "hello world"::uppercase()::replace("WORLD", "earth"::uppercase())
    // Expected: "HELLO EARTH"
    @Test
    public void testScenarioQ_chainedContextCallWithNestedContextCallArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "\"hello world\"::uppercase()::replace(\"WORLD\", \"earth\"::uppercase())"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("HELLO EARTH", result.getCompileResult(),
                "Compiled result should be 'HELLO EARTH', got: " + result.getCompileResult());
    }

    // Scenario R: 多个嵌套上下文调用参数 + 结果用于赋值和后续操作
    @Test
    public void testScenarioR_complexNestedContextCallsWithAssignment() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "x = \"hello world\"::replace(\"hello\"::substring(0, 2), \"world\"::substring(0, 2)); &x"
        );
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        // replace("he", "wo") on "hello world" => "wollo world"
        assertEquals("wollo world", result.getCompileResult(),
                "Compiled result should be 'wollo world', got: " + result.getCompileResult());
    }

    // Scenario S: 使用 import + 多层上下文调用
    // time::formatTimestamp(...) 结果 :: split("-") :: get(0)
    // 验证嵌套调用链正确恢复 target
    @Test
    public void testScenarioS_importedFuncChainedWithContextCallArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:time'; time::formatTimestamp(1755611940830L)::split(\"-\")::get(\"xx\"::length())"
        );
        // "xx"::length() = 2, split("-") gives ["2025", "08", "19 21:59:00"], get(2) = "19 21:59:00"
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("19 21:59:00", result.getCompileResult(),
                "Compiled result should be '19 21:59:00', got: " + result.getCompileResult());
    }

    // Scenario T: 用户定义函数调用用户定义函数，最后参数是上下文调用
    @Test
    public void testScenarioT_nestedUserFuncsWithContextCallLastArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "def inner(x) = &x * 10; def outer(a, b) = inner(&a) + inner(&b); outer(2, [1,2,3]::size())"
        );
        // inner(2)=20, inner(3)=30, outer=50
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(50, result.getCompileResult(),
                "Compiled result should be 50, got: " + result.getCompileResult());
    }

    // Scenario U: 注入变量作为 target，编译器无法推断类型
    @Test
    public void testScenarioU_injectedVarTarget() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilentWithEnv(
                "&target::replace(\"world\", \"hello\"::substring(0, 2))",
                env -> env.defineRootVariable("target", "hello world")
        );
        System.out.println("U: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "U: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario V: 两个注入变量 target，两层都走 deferred
    @Test
    public void testScenarioV_bothInjectedVarTargets() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilentWithEnv(
                "&outer::replace(\"world\", &inner::substring(0, 2))",
                env -> {
                    env.defineRootVariable("outer", "hello world");
                    env.defineRootVariable("inner", "hello");
                }
        );
        System.out.println("V: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "V: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario W: 注入变量 target + 多参数 + 最后参数是上下文调用
    @Test
    public void testScenarioW_injectedVarMultiArg() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilentWithEnv(
                "&target::substring(0, &inner::length())",
                env -> {
                    env.defineRootVariable("target", "hello world");
                    env.defineRootVariable("inner", "hello");
                }
        );
        System.out.println("W: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "W: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario X: 同一注入变量作为内外 target（模拟 feishu::func1(..., feishu::func2(...))）
    @Test
    public void testScenarioX_sameInjectedVarBothTargets() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilentWithEnv(
                "&obj::replace(\"world\", &obj::substring(0, 2))",
                env -> env.defineRootVariable("obj", "hello world")
        );
        System.out.println("X: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "X: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario Y: 注入变量 + 5 参数（模拟用户场景: 5 参数 + 最后参数是函数调用）
    @Test
    public void testScenarioY_injectedVar5ArgsLastContextCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilentWithEnv(
                "def myFunc(a, b, c, d, e) = &a; myFunc(\"x\", \"y\", \"z\", \"w\", &target::length())",
                env -> env.defineRootVariable("target", "hello")
        );
        System.out.println("Y: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Y: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario Z: 注入自定义对象 + NativeFunction 扩展
    @Test
    public void testScenarioZ_injectedMapTarget() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilentWithEnv(
                "&data::size()",
                env -> env.defineRootVariable("data", map)
        );
        System.out.println("Z: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "Z: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario AA: 注入 Map target，上下文调用参数也是注入变量的上下文调用
    @Test
    public void testScenarioAA_injectedMapWithContextCallArg() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilentWithEnv(
                "def myFunc(a, b, c) = &a; myFunc(\"first\", \"second\", &data::size())",
                env -> env.defineRootVariable("data", map)
        );
        System.out.println("AA: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "AA: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario BB: 自定义 NativeFunction 扩展 + 嵌套上下文调用（核心复现场景）
    // 模拟 feishu::searchRecords("a", "b", "c", "d", feishu::createFilter("x", "y", "z"))
    @Test
    public void testScenarioBB_nativeFuncNestedContextCall() {
        org.tabooproject.fluxon.runtime.FluxonRuntime runtime = org.tabooproject.fluxon.runtime.FluxonRuntime.getInstance();
        org.tabooproject.fluxon.runtime.NativeFunction<java.util.concurrent.atomic.AtomicInteger> searchFunc =
                new org.tabooproject.fluxon.runtime.NativeFunction<>(
                        "search",
                        org.tabooproject.fluxon.runtime.FunctionSignature.returnsObject()
                                .params(org.tabooproject.fluxon.runtime.Type.OBJECT,
                                        org.tabooproject.fluxon.runtime.Type.OBJECT,
                                        org.tabooproject.fluxon.runtime.Type.OBJECT,
                                        org.tabooproject.fluxon.runtime.Type.OBJECT,
                                        org.tabooproject.fluxon.runtime.Type.OBJECT),
                        ctx -> ctx.setReturnRef("search_" + ctx.getRef(0)));
        org.tabooproject.fluxon.runtime.NativeFunction<java.util.concurrent.atomic.AtomicInteger> filterFunc =
                new org.tabooproject.fluxon.runtime.NativeFunction<>(
                        "filter",
                        org.tabooproject.fluxon.runtime.FunctionSignature.returnsObject()
                                .params(org.tabooproject.fluxon.runtime.Type.OBJECT,
                                        org.tabooproject.fluxon.runtime.Type.OBJECT,
                                        org.tabooproject.fluxon.runtime.Type.OBJECT),
                        ctx -> ctx.setReturnRef("filter_" + ctx.getRef(0)));
        runtime.registerExtensionFunction(java.util.concurrent.atomic.AtomicInteger.class, searchFunc);
        runtime.registerExtensionFunction(java.util.concurrent.atomic.AtomicInteger.class, filterFunc);
        try {
            FluxonTestUtil.TestResult result = FluxonTestUtil.runSilentWithEnv(
                    "&svc::search(\"a\", \"b\", \"c\", \"d\", &svc::filter(\"x\", \"y\", \"z\"))",
                    env -> env.defineRootVariable("svc", new java.util.concurrent.atomic.AtomicInteger(0))
            );
            System.out.println("BB: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
            assertEquals("search_a", result.getInterpretResult(),
                    "BB interpret: expected search_a, got " + result.getInterpretResult());
            assertEquals("search_a", result.getCompileResult(),
                    "BB compile: expected search_a, got " + result.getCompileResult());
        } finally {
            runtime.unregisterExtensionFunction(java.util.concurrent.atomic.AtomicInteger.class, "search", searchFunc);
            runtime.unregisterExtensionFunction(java.util.concurrent.atomic.AtomicInteger.class, "filter", filterFunc);
        }
    }

    // Scenario CC: 自定义 NativeFunction 扩展 + 2 参数 + 最后参数是上下文调用
    @Test
    public void testScenarioCC_nativeFuncTwoArgsLastContextCall() {
        org.tabooproject.fluxon.runtime.FluxonRuntime runtime = org.tabooproject.fluxon.runtime.FluxonRuntime.getInstance();
        org.tabooproject.fluxon.runtime.NativeFunction<java.util.concurrent.atomic.AtomicInteger> searchFunc =
                new org.tabooproject.fluxon.runtime.NativeFunction<>(
                        "search2",
                        org.tabooproject.fluxon.runtime.FunctionSignature.returnsObject()
                                .params(org.tabooproject.fluxon.runtime.Type.OBJECT,
                                        org.tabooproject.fluxon.runtime.Type.OBJECT),
                        ctx -> ctx.setReturnRef("search2_" + ctx.getRef(0)));
        org.tabooproject.fluxon.runtime.NativeFunction<java.util.concurrent.atomic.AtomicInteger> filterFunc =
                new org.tabooproject.fluxon.runtime.NativeFunction<>(
                        "filter2",
                        org.tabooproject.fluxon.runtime.FunctionSignature.returnsObject()
                                .params(org.tabooproject.fluxon.runtime.Type.OBJECT),
                        ctx -> ctx.setReturnRef("filter2_" + ctx.getRef(0)));
        runtime.registerExtensionFunction(java.util.concurrent.atomic.AtomicInteger.class, searchFunc);
        runtime.registerExtensionFunction(java.util.concurrent.atomic.AtomicInteger.class, filterFunc);
        try {
            FluxonTestUtil.TestResult result = FluxonTestUtil.runSilentWithEnv(
                    "&svc::search2(\"a\", &svc::filter2(\"x\"))",
                    env -> env.defineRootVariable("svc", new java.util.concurrent.atomic.AtomicInteger(0))
            );
            System.out.println("CC: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
            assertEquals("search2_a", result.getInterpretResult(),
                    "CC interpret: expected search2_a, got " + result.getInterpretResult());
            assertEquals("search2_a", result.getCompileResult(),
                    "CC compile: expected search2_a, got " + result.getCompileResult());
        } finally {
            runtime.unregisterExtensionFunction(java.util.concurrent.atomic.AtomicInteger.class, "search2", searchFunc);
            runtime.unregisterExtensionFunction(java.util.concurrent.atomic.AtomicInteger.class, "filter2", filterFunc);
        }
    }

    // Scenario DD: 自定义扩展 + 不同 target 类型 (StringBuilder)
    @Test
    public void testScenarioDD_nativeFuncDifferentTargetType() {
        org.tabooproject.fluxon.runtime.FluxonRuntime runtime = org.tabooproject.fluxon.runtime.FluxonRuntime.getInstance();
        org.tabooproject.fluxon.runtime.NativeFunction<StringBuilder> searchFunc =
                new org.tabooproject.fluxon.runtime.NativeFunction<>(
                        "sbSearch",
                        org.tabooproject.fluxon.runtime.FunctionSignature.returnsObject()
                                .params(org.tabooproject.fluxon.runtime.Type.OBJECT,
                                        org.tabooproject.fluxon.runtime.Type.OBJECT,
                                        org.tabooproject.fluxon.runtime.Type.OBJECT),
                        ctx -> ctx.setReturnRef("SB_SEARCH_RESULT"));
        org.tabooproject.fluxon.runtime.NativeFunction<StringBuilder> filterFunc =
                new org.tabooproject.fluxon.runtime.NativeFunction<>(
                        "sbFilter",
                        org.tabooproject.fluxon.runtime.FunctionSignature.returnsObject()
                                .params(org.tabooproject.fluxon.runtime.Type.OBJECT,
                                        org.tabooproject.fluxon.runtime.Type.OBJECT,
                                        org.tabooproject.fluxon.runtime.Type.OBJECT),
                        ctx -> ctx.setReturnRef("SB_FILTER_RESULT"));
        runtime.registerExtensionFunction(StringBuilder.class, searchFunc);
        runtime.registerExtensionFunction(StringBuilder.class, filterFunc);
        try {
            FluxonTestUtil.TestResult result = FluxonTestUtil.runSilentWithEnv(
                    "&svc::sbSearch(\"first\", \"second\", &svc::sbFilter(\"field\", \"op\", [\"val\"]))",
                    env -> env.defineRootVariable("svc", new StringBuilder("test"))
            );
            System.out.println("DD: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
            assertEquals("SB_SEARCH_RESULT", result.getInterpretResult(),
                    "DD interpret: expected SB_SEARCH_RESULT, got " + result.getInterpretResult());
            assertEquals("SB_SEARCH_RESULT", result.getCompileResult(),
                    "DD compile: expected SB_SEARCH_RESULT, got " + result.getCompileResult());
        } finally {
            runtime.unregisterExtensionFunction(StringBuilder.class, "sbSearch", searchFunc);
            runtime.unregisterExtensionFunction(StringBuilder.class, "sbFilter", filterFunc);
        }
    }

    // Scenario EE: time 模块 - 嵌套上下文调用，最后参数是 time::now()
    // 注意：getNow → now（StringUtils.transformMethodName 去掉 get 前缀）
    @Test
    public void testScenarioEE_timeModuleNestedContextCall() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:time'; time::isBetween(1000L, 0L, time::now())"
        );
        System.out.println("EE: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(true, result.getInterpretResult(), "EE interpret");
        assertEquals(true, result.getCompileResult(), "EE compile");
    }

    // Scenario FF: time 模块 - daysBetween 最后参数是 time::now()
    @Test
    public void testScenarioFF_timeModuleDaysBetween() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:time'; time::daysBetween(0L, time::now())"
        );
        System.out.println("FF: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "FF: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario GG: time 模块 - formatTimestamp(addDays(now(), -1))
    @Test
    public void testScenarioGG_timeModuleTripleNested() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:time'; time::formatTimestamp(time::addDays(time::now(), -1))"
        );
        System.out.println("GG: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "GG: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario HH: time 模块 - addDays 第二参数是 time::day()
    @Test
    public void testScenarioHH_timeModuleAddDaysNested() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:time'; r = time::addDays(1755611940830L, time::day()); &r > 0"
        );
        System.out.println("HH: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "HH: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    /**
     * 模拟 feishu 模块的测试类
     * searchRecords 和 createFilter 均为 @Export 方法，不以 get 开头，名称不会被转换
     */
    public static class TestModule {

        public static final TestModule INSTANCE = new TestModule();
        public static final Type TYPE = Type.fromClass(TestModule.class);

        @Export
        public String searchRecords(String appId, String token, String tableId, Object fields, Object filter) {
            return "SEARCH_RESULT_" + appId;
        }

        @Export
        public String createFilter(String field, String operator, Object values) {
            return "FILTER_" + field;
        }

        @Export
        public String twoArgFunc(String first, Object second) {
            return "TWO_ARG_" + first;
        }

        // 返回 long 的方法（模拟 time::now() 的返回类型）
        @Export
        public long getTimestamp() {
            return 1234567890L;
        }

        // 接收 long 参数的方法（模拟 time::daysBetween 等）
        @Export
        public long computeWithLong(long a, long b) {
            return a + b;
        }

        // 返回 int 的方法
        @Export
        public int getCount() {
            return 42;
        }

        // 接收 int 最后参数的方法
        @Export
        public String searchWithInt(String appId, String token, int limit) {
            return "SEARCH_INT_" + appId + "_" + limit;
        }
    }

    private static boolean testModuleRegistered = false;

    /**
     * 确保 TestModule 已注册到运行时
     */
    private static void ensureTestModuleRegistered() {
        if (testModuleRegistered) return;
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction("fs:testmod", "testmod",
                FunctionSignature.returns(TestModule.TYPE).noParams(),
                context -> context.setReturnRef(TestModule.INSTANCE));
        runtime.getExportRegistry().registerClass(TestModule.class, "fs:testmod");
        testModuleRegistered = true;
    }

    // Scenario II: createFilter 单独调用（基线，应正常工作）
    @Test
    public void testScenarioII_createFilterAlone() {
        ensureTestModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:testmod'; testmod::createFilter(\"field\", \"contains\", [\"val\"])"
        );
        System.out.println("II: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("FILTER_field", result.getInterpretResult(), "II interpret");
        assertEquals("FILTER_field", result.getCompileResult(), "II compile");
    }

    // Scenario JJ: searchRecords 使用字面量最后参数（基线，应正常工作）
    @Test
    public void testScenarioJJ_searchRecordsLiteralLastArg() {
        ensureTestModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:testmod'; testmod::searchRecords(\"app\", \"token\", \"table\", [\"a\",\"b\"], \"literal\")"
        );
        System.out.println("JJ: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("SEARCH_RESULT_app", result.getInterpretResult(), "JJ interpret");
        assertEquals("SEARCH_RESULT_app", result.getCompileResult(), "JJ compile");
    }

    // Scenario KK: 核心 Bug 复现 - searchRecords 最后参数是 createFilter 上下文调用
    // 模拟用户的 feishu::searchRecords(..., feishu::createFilter(...))
    // Expected: "SEARCH_RESULT_app"
    // Bug: "FILTER_field"（返回最后参数的值）
    @Test
    public void testScenarioKK_searchRecordsWithCreateFilterLastArg() {
        ensureTestModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:testmod'; testmod::searchRecords(\"app\", \"token\", \"table\", [\"a\",\"b\"], testmod::createFilter(\"field\", \"contains\", [\"val\"]))"
        );
        System.out.println("KK: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("SEARCH_RESULT_app", result.getInterpretResult(),
                "KK interpret: expected SEARCH_RESULT_app, got " + result.getInterpretResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "KK: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario LL: 两参数函数，最后参数是 createFilter 上下文调用
    @Test
    public void testScenarioLL_twoArgFuncWithCreateFilterLastArg() {
        ensureTestModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:testmod'; testmod::twoArgFunc(\"first\", testmod::createFilter(\"f\", \"op\", [\"v\"]))"
        );
        System.out.println("LL: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("TWO_ARG_first", result.getInterpretResult(),
                "LL interpret: expected TWO_ARG_first, got " + result.getInterpretResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "LL: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario MM: 赋值后使用结果，验证返回值不被污染
    @Test
    public void testScenarioMM_assignedSearchRecordsWithNestedCall() {
        ensureTestModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:testmod'; r = testmod::searchRecords(\"myApp\", \"tok\", \"tbl\", [\"x\"], testmod::createFilter(\"col\", \"eq\", [\"v\"])); &r"
        );
        System.out.println("MM: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("SEARCH_RESULT_myApp", result.getInterpretResult(),
                "MM interpret: expected SEARCH_RESULT_myApp, got " + result.getInterpretResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "MM: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario NN: 结果用于后续运算
    @Test
    public void testScenarioNN_searchRecordsResultInExpression() {
        ensureTestModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:testmod'; testmod::searchRecords(\"app\", \"tok\", \"tbl\", [\"x\"], testmod::createFilter(\"f\", \"op\", [\"v\"])) + \"_DONE\""
        );
        System.out.println("NN: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("SEARCH_RESULT_app_DONE", result.getInterpretResult(),
                "NN interpret: expected SEARCH_RESULT_app_DONE, got " + result.getInterpretResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "NN: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario OO: 返回 long 的函数作为另一个函数的最后参数（模拟 time::now() 场景）
    // getTimestamp → timestamp（transformMethodName 去掉 get 前缀）
    @Test
    public void testScenarioOO_longReturnNestedAsLastArg() {
        ensureTestModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:testmod'; testmod::computeWithLong(100L, testmod::timestamp())"
        );
        System.out.println("OO: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "OO: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario PP: 返回 int 的函数作为最后参数
    // getCount → count（transformMethodName 去掉 get 前缀）
    @Test
    public void testScenarioPP_intReturnNestedAsLastArg() {
        ensureTestModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:testmod'; testmod::searchWithInt(\"app\", \"token\", testmod::count())"
        );
        System.out.println("PP: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("SEARCH_INT_app_42", result.getInterpretResult(),
                "PP interpret: expected SEARCH_INT_app_42, got " + result.getInterpretResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "PP: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario QQ: 5 参数 + 最后参数是返回 long 的嵌套调用（最接近原始 bug 场景）
    @Test
    public void testScenarioQQ_fiveArgsWithLongNestedLastArg() {
        ensureTestModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:testmod'; testmod::searchRecords(\"app\", \"token\", \"table\", [\"a\"], testmod::timestamp())"
        );
        System.out.println("QQ: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("SEARCH_RESULT_app", result.getInterpretResult(),
                "QQ interpret: expected SEARCH_RESULT_app, got " + result.getInterpretResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "QQ: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    /**
     * 模拟飞书模块，精确匹配用户上报的 feishu::searchRecords(..., feishu::createFilter(...)) 模式
     */
    public static class FeishuModule {

        public static final FeishuModule INSTANCE = new FeishuModule();

        @Export
        public String searchRecords(String appId, String token, String tableId, Object fields, Object filter) {
            return "SEARCH_RESULT_" + appId;
        }

        @Export
        public String createFilter(String field, String operator, Object values) {
            return "FILTER_" + field;
        }
    }

    private static boolean feishuModuleRegistered = false;

    private static void ensureFeishuModuleRegistered() {
        if (feishuModuleRegistered) return;
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        runtime.registerFunction("fs:feishu", "feishu",
                FunctionSignature.returns(Type.fromClass(FeishuModule.class)).noParams(),
                context -> context.setReturnRef(FeishuModule.INSTANCE));
        runtime.getExportRegistry().registerClass(FeishuModule.class, "fs:feishu");
        feishuModuleRegistered = true;
    }

    // Scenario RR: feishu createFilter 单独调用（基线）
    @Test
    public void testScenarioRR_feishuCreateFilterAlone() {
        ensureFeishuModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:feishu'; feishu::createFilter(\"field\", \"contains\", [\"val\"])"
        );
        System.out.println("RR: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("FILTER_field", result.getInterpretResult(), "RR interpret");
        assertEquals("FILTER_field", result.getCompileResult(), "RR compile");
    }

    // Scenario SS: feishu searchRecords 字面量最后参数（基线）
    @Test
    public void testScenarioSS_feishuSearchRecordsLiteralLastArg() {
        ensureFeishuModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:feishu'; feishu::searchRecords(\"app\", \"tok\", \"tbl\", [\"a\"], \"literal\")"
        );
        System.out.println("SS: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("SEARCH_RESULT_app", result.getInterpretResult(), "SS interpret");
        assertEquals("SEARCH_RESULT_app", result.getCompileResult(), "SS compile");
    }

    // Scenario TT: 核心 Bug 复现 - feishu::searchRecords(..., feishu::createFilter(...))
    // Expected: "SEARCH_RESULT_app"
    // Bug: "FILTER_field"（返回内层函数的值）
    @Test
    public void testScenarioTT_feishuSearchRecordsWithCreateFilterLastArg() {
        ensureFeishuModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:feishu'; feishu::searchRecords(\"app\", \"tok\", \"tbl\", [\"a\",\"b\"], feishu::createFilter(\"field\", \"contains\", [\"val\"]))"
        );
        System.out.println("TT: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("SEARCH_RESULT_app", result.getInterpretResult(),
                "TT interpret: expected SEARCH_RESULT_app, got " + result.getInterpretResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "TT BUG CHECK: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario UU: feishu 两参数，最后参数是 createFilter 上下文调用
    @Test
    public void testScenarioUU_feishuSearchRecordsTwoArgsWithCreateFilter() {
        ensureFeishuModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:feishu'; feishu::searchRecords(\"app\", \"tok\", \"tbl\", [\"a\"], feishu::createFilter(\"f2\", \"eq\", [\"v2\"]))"
        );
        System.out.println("UU: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("SEARCH_RESULT_app", result.getInterpretResult(),
                "UU interpret: expected SEARCH_RESULT_app, got " + result.getInterpretResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "UU BUG CHECK: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario VV: feishu 赋值后使用（排除表达式尾部干扰）
    @Test
    public void testScenarioVV_feishuAssignedSearchRecordsWithNestedCall() {
        ensureFeishuModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "import 'fs:feishu'; r = feishu::searchRecords(\"app\", \"tok\", \"tbl\", [\"a\"], feishu::createFilter(\"f\", \"op\", [\"v\"])); &r"
        );
        System.out.println("VV: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals("SEARCH_RESULT_app", result.getInterpretResult(),
                "VV interpret: expected SEARCH_RESULT_app, got " + result.getInterpretResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "VV BUG CHECK: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // 与上面 FeishuModule 的区别：
    // 1. returnsObject() 而非 Type.fromClass(...)，返回 Type.OBJECT
    // 2. 无命名空间（namespace = null），无需 import
    // 3. searchRecords 返回 List<Map>，createFilter 返回 Map（都是 Object 类型）
    // 4. searchRecords 最后一个参数标记 @Optional

    /**
     * 精确模拟用户的飞书模块注册模式
     * 返回 Object 类型（List/Map），无命名空间，@Optional 参数
     */
    public static class FeishuObjectModule {

        public static final FeishuObjectModule INSTANCE = new FeishuObjectModule();

        // 模拟 searchRecords 返回 List<Map<String, Any>>
        @Export
        public List<Map<String, Object>> searchRecords(String appToken, String accessToken, String tableId, Object fieldNames, @Optional Object filter) {
            List<Map<String, Object>> result = new ArrayList<>();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("appToken", appToken);
            row.put("filterUsed", filter != null);
            result.add(row);
            return result;
        }

        // 模拟 createFilter 返回 Map<String, Any>
        @Export
        public Map<String, Object> createFilter(String fieldName, String operator, Object value) {
            Map<String, Object> filter = new LinkedHashMap<>();
            filter.put("fieldName", fieldName);
            filter.put("operator", operator);
            filter.put("value", value);
            return filter;
        }
    }

    private static boolean feishuObjectModuleRegistered = false;

    private static void ensureFeishuObjectModuleRegistered() {
        if (feishuObjectModuleRegistered) return;
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        // 用户代码模式：无命名空间，returnsObject()
        runtime.registerFunction("feishuObj",
                FunctionSignature.returnsObject().noParams(),
                context -> context.setReturnRef(FeishuObjectModule.INSTANCE));
        // 无命名空间注册
        runtime.getExportRegistry().registerClass(FeishuObjectModule.class);
        feishuObjectModuleRegistered = true;
    }

    // Scenario WW: feishuObj createFilter 单独调用（基线）
    @Test
    public void testScenarioWW_feishuObjCreateFilterAlone() {
        ensureFeishuObjectModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "feishuObj::createFilter(\"field\", \"contains\", [\"val\"])"
        );
        System.out.println("WW: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertNotNull(result.getInterpretResult(), "WW interpret should not be null");
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "WW: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario XX: feishuObj searchRecords 字面量参数（基线）
    @Test
    public void testScenarioXX_feishuObjSearchRecordsLiteralArgs() {
        ensureFeishuObjectModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "feishuObj::searchRecords(\"app\", \"tok\", \"tbl\", [\"a\",\"b\"], \"literalFilter\")"
        );
        System.out.println("XX: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertNotNull(result.getInterpretResult(), "XX interpret should not be null");
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "XX: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario YY: 核心 Bug 复现 - feishuObj::searchRecords(..., feishuObj::createFilter(...))
    // Object 返回类型 + 无命名空间 + @Optional 参数
    // Expected: searchRecords 的返回值 (List)
    // Bug: createFilter 的返回值 (Map)
    @Test
    public void testScenarioYY_feishuObjSearchRecordsWithCreateFilterNested() {
        ensureFeishuObjectModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "feishuObj::searchRecords(\"app\", \"tok\", \"tbl\", [\"a\",\"b\"], feishuObj::createFilter(\"field\", \"contains\", [\"val\"]))"
        );
        System.out.println("YY: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertNotNull(result.getInterpretResult(), "YY interpret should not be null");
        assertTrue(result.getInterpretResult() instanceof List,
                "YY interpret: expected List, got " + result.getInterpretResult().getClass().getSimpleName());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "YY BUG CHECK: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario ZZ: feishuObj 赋值后使用
    @Test
    public void testScenarioZZ_feishuObjAssignedWithNestedCall() {
        ensureFeishuObjectModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "r = feishuObj::searchRecords(\"app\", \"tok\", \"tbl\", [\"a\"], feishuObj::createFilter(\"f\", \"op\", [\"v\"])); &r"
        );
        System.out.println("ZZ: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertNotNull(result.getInterpretResult(), "ZZ interpret should not be null");
        assertTrue(result.getInterpretResult() instanceof List,
                "ZZ interpret: expected List, got " + result.getInterpretResult().getClass().getSimpleName());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "ZZ BUG CHECK: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario AAA: feishuObj searchRecords 无 filter 参数 (@Optional 省略)
    @Test
    public void testScenarioAAA_feishuObjSearchRecordsNoFilter() {
        ensureFeishuObjectModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "feishuObj::searchRecords(\"app\", \"tok\", \"tbl\", [\"a\"])"
        );
        System.out.println("AAA: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertNotNull(result.getInterpretResult(), "AAA interpret should not be null");
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "AAA: Interpret and compile should match. interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario BBB: feishuObj 结果用于后续操作（::size()）
    @Test
    public void testScenarioBBB_feishuObjResultUsedInChain() {
        ensureFeishuObjectModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "feishuObj::searchRecords(\"app\", \"tok\", \"tbl\", [\"a\"], feishuObj::createFilter(\"f\", \"op\", [\"v\"]))::size()"
        );
        System.out.println("BBB: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        assertEquals(1, result.getInterpretResult(),
                "BBB interpret: expected 1, got " + result.getInterpretResult());
        assertEquals(result.getInterpretResult(), result.getCompileResult(),
                "BBB BUG CHECK: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
    }

    // Scenario CCC: async def 中嵌套调用，赋值后读取结果
    // 精确模拟用户代码模式：async def run { npcs = feishu::searchRecords(..., feishu::createFilter(...)); &npcs }
    @Test
    public void testScenarioCCC_asyncDefWithNestedCall() throws Exception {
        ensureFeishuObjectModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "@except\n" +
                "async def run {\n" +
                "  npcs = feishuObj::searchRecords(\"app\", \"tok\", \"tbl\", [\"a\",\"b\"], feishuObj::createFilter(\"field\", \"contains\", [\"val\"]))\n" +
                "  &npcs\n" +
                "}\n" +
                "run()"
        );
        System.out.println("CCC: interpret=" + result.getInterpretResult() + " compile=" + result.getCompileResult());
        // async 函数返回 Future，需要 .get() 获取实际值
        Object interpretVal = resolveFuture(result.getInterpretResult());
        Object compileVal = resolveFuture(result.getCompileResult());
        System.out.println("CCC resolved: interpret=" + interpretVal + " compile=" + compileVal);
        assertNotNull(interpretVal, "CCC interpret should not be null");
        assertTrue(interpretVal instanceof List, "CCC interpret: expected List, got " + (interpretVal == null ? "null" : interpretVal.getClass().getSimpleName()));
        assertEquals(interpretVal, compileVal,
                "CCC BUG CHECK: interpret=" + interpretVal + " compile=" + compileVal);
    }

    // Scenario DDD: async def 中嵌套调用（无 @Optional，简化版本）
    @Test
    public void testScenarioDDD_asyncDefWithNestedCallSimple() throws Exception {
        ensureFeishuObjectModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "@except\n" +
                "async def run {\n" +
                "  r = feishuObj::createFilter(\"f\", \"op\", [\"v\"])\n" +
                "  &r\n" +
                "}\n" +
                "run()"
        );
        Object interpretVal = resolveFuture(result.getInterpretResult());
        Object compileVal = resolveFuture(result.getCompileResult());
        System.out.println("DDD resolved: interpret=" + interpretVal + " compile=" + compileVal);
        assertNotNull(interpretVal, "DDD interpret should not be null");
        assertTrue(interpretVal instanceof Map, "DDD interpret: expected Map, got " + (interpretVal == null ? "null" : interpretVal.getClass().getSimpleName()));
        assertEquals(interpretVal, compileVal,
                "DDD BUG CHECK: interpret=" + interpretVal + " compile=" + compileVal);
    }

    // Scenario EEE: async def 中变量传递 + 嵌套调用（模拟 &token 场景）
    @Test
    public void testScenarioEEE_asyncDefWithVarAndNestedCall() throws Exception {
        ensureFeishuObjectModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "@except\n" +
                "async def run {\n" +
                "  token = \"myToken\"\n" +
                "  npcs = feishuObj::searchRecords(\"app\", &token, \"tbl\", [\"a\"], feishuObj::createFilter(\"field\", \"contains\", [\"val\"]))\n" +
                "  &npcs\n" +
                "}\n" +
                "run()"
        );
        Object interpretVal = resolveFuture(result.getInterpretResult());
        Object compileVal = resolveFuture(result.getCompileResult());
        System.out.println("EEE resolved: interpret=" + interpretVal + " compile=" + compileVal);
        assertNotNull(interpretVal, "EEE interpret should not be null");
        assertTrue(interpretVal instanceof List, "EEE interpret: expected List, got " + (interpretVal == null ? "null" : interpretVal.getClass().getSimpleName()));
        assertEquals(interpretVal, compileVal,
                "EEE BUG CHECK: interpret=" + interpretVal + " compile=" + compileVal);
    }

    // Scenario FFF: async def 中嵌套调用后进行 for 迭代
    @Test
    public void testScenarioFFF_asyncDefWithNestedCallAndIteration() throws Exception {
        ensureFeishuObjectModuleRegistered();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "@except\n" +
                "async def run {\n" +
                "  npcs = feishuObj::searchRecords(\"app\", \"tok\", \"tbl\", [\"a\"], feishuObj::createFilter(\"f\", \"op\", [\"v\"]))\n" +
                "  count = 0\n" +
                "  for i in &npcs {\n" +
                "    count = &count + 1\n" +
                "  }\n" +
                "  &count\n" +
                "}\n" +
                "run()"
        );
        Object interpretVal = resolveFuture(result.getInterpretResult());
        Object compileVal = resolveFuture(result.getCompileResult());
        System.out.println("FFF resolved: interpret=" + interpretVal + " compile=" + compileVal);
        assertEquals(1, interpretVal, "FFF interpret: expected 1 iteration, got " + interpretVal);
        assertEquals(interpretVal, compileVal,
                "FFF BUG CHECK: interpret=" + interpretVal + " compile=" + compileVal);
    }

    /**
     * 解析 Future/CompletableFuture 的结果
     */
    private static Object resolveFuture(Object value) throws Exception {
        if (value instanceof java.util.concurrent.Future) {
            return ((java.util.concurrent.Future<?>) value).get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        return value;
    }
}
