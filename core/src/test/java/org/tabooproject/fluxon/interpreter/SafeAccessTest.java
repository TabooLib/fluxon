package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全访问操作符测试
 * 测试 ?. (安全成员访问) 和 ?:: (安全上下文调用) 操作符
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class SafeAccessTest {

    // ========== 安全成员访问 (?.) 测试 ==========

    @Test
    public void testSafeMemberAccessOnNull() {
        // 当 target 为 null 时，?. 应该返回 null 而不是抛出 NPE
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("obj = null; &obj?.toString()");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }

    @Test
    public void testSafeMemberAccessOnNonNull() {
        // 当 target 不为 null 时，?. 应该正常访问成员
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("obj = 'hello'; &obj?.length()");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());
    }

    @Test
    public void testSafeFieldAccessOnNull() {
        // 字段访问也支持安全访问
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("obj = null; &obj?.class");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }

    @Test
    public void testSafeFieldAccessOnNonNull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("obj = 'hello'; &obj?.class");
        assertEquals(String.class, result.getInterpretResult());
        assertEquals(String.class, result.getCompileResult());
    }

    @Test
    public void testChainedSafeMemberAccess() {
        // 链式安全访问：a?.b?.c
        FluxonTestUtil.TestResult result;

        // 第一个为 null，整个链返回 null
        result = FluxonTestUtil.runSilent("a = null; &a?.toString()?.length()");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());

        // 正常访问
        result = FluxonTestUtil.runSilent("a = 'hello'; &a?.toString()?.length()");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());
    }

    @Test
    public void testMixedSafeAndUnsafeAccess() {
        // 混合安全和非安全访问
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("obj = 'hello'; &obj?.toUpperCase().toLowerCase()");
        assertEquals("hello", result.getInterpretResult());
        assertEquals("hello", result.getCompileResult());
    }

    @Test
    public void testSafeAccessWithMethodArguments() {
        // 带参数的方法调用
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("obj = 'hello world'; &obj?.substring(0, 5)");
        assertEquals("hello", result.getInterpretResult());
        assertEquals("hello", result.getCompileResult());

        result = FluxonTestUtil.runSilent("obj = null; &obj?.substring(0, 5)");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }

    // ========== 安全上下文调用 (?::) 测试 ==========

    @Test
    public void testSafeContextCallOnNull() {
        // 当 target 为 null 时，?:: 应该返回 null 而不是执行上下文
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("obj = null; &obj ?:: uppercase()");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }

    @Test
    public void testSafeContextCallOnNonNull() {
        // 当 target 不为 null 时，?:: 应该正常执行上下文
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("obj = 'hello'; &obj ?:: uppercase()");
        assertEquals("HELLO", result.getInterpretResult());
        assertEquals("HELLO", result.getCompileResult());
    }

    @Test
    public void testSafeContextCallWithBlock() {
        // 安全上下文调用块
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("obj = null; &obj ?:: { uppercase() }");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());

        result = FluxonTestUtil.runSilent("obj = 'hello'; &obj ?:: { uppercase() }");
        assertEquals("HELLO", result.getInterpretResult());
        assertEquals("HELLO", result.getCompileResult());
    }

    @Test
    public void testChainedSafeContextCall() {
        // 链式安全上下文调用
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("obj = null; &obj ?:: uppercase() ?:: split('')");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());

        result = FluxonTestUtil.runSilent("obj = 'hi'; &obj ?:: uppercase()");
        assertEquals("HI", result.getInterpretResult());
        assertEquals("HI", result.getCompileResult());
    }

    // ========== 混合安全访问测试 ==========

    @Test
    public void testMixedSafeOperators() {
        // 混合使用 ?. 和 ?::
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("obj = null; &obj?.toString() ?:: uppercase()");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());

        result = FluxonTestUtil.runSilent("obj = 'hello'; &obj?.toString() ?:: uppercase()");
        assertEquals("HELLO", result.getInterpretResult());
        assertEquals("HELLO", result.getCompileResult());
    }

    // ========== Elvis 操作符组合测试 ==========

    @Test
    public void testSafeAccessWithElvis() {
        // 安全访问与 Elvis 组合
        FluxonTestUtil.TestResult result;

        // ?. 返回 null 时使用 Elvis 默认值
        result = FluxonTestUtil.runSilent("obj = null; &obj?.length() ?: 0");
        assertEquals(0, result.getInterpretResult());
        assertEquals(0, result.getCompileResult());

        result = FluxonTestUtil.runSilent("obj = 'hello'; &obj?.length() ?: 0");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());

        // ?:: 返回 null 时使用 Elvis 默认值
        result = FluxonTestUtil.runSilent("obj = null; (&obj ?:: uppercase()) ?: 'default'");
        assertEquals("default", result.getInterpretResult());
        assertEquals("default", result.getCompileResult());

        result = FluxonTestUtil.runSilent("obj = 'hello'; (&obj ?:: uppercase()) ?: 'default'");
        assertEquals("HELLO", result.getInterpretResult());
        assertEquals("HELLO", result.getCompileResult());
    }

    // ========== 条件表达式测试 ==========

    @Test
    public void testSafeAccessInCondition() {
        FluxonTestUtil.TestResult result;

        // 在条件表达式中使用安全访问
        result = FluxonTestUtil.runSilent("obj = null; if &obj?.length() == null then 'null' else 'not null'");
        assertEquals("null", result.getInterpretResult());
        assertEquals("null", result.getCompileResult());

        result = FluxonTestUtil.runSilent("obj = 'hello'; if &obj?.length() == null then 'null' else 'not null'");
        assertEquals("not null", result.getInterpretResult());
        assertEquals("not null", result.getCompileResult());
    }

    // ========== 普通成员访问对比测试 ==========

    @Test
    public void testNormalMemberAccessOnNullThrows() {
        // 普通成员访问 (.) 在 target 为 null 时应该抛出异常
        assertThrows(NullPointerException.class, () -> {
            FluxonTestUtil.runSilent("obj = null; &obj.toString()");
        });
    }

    // ========== 列表和集合测试 ==========

    @Test
    public void testSafeAccessOnList() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("list = null; &list?.size()");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = [1,2,3]; &list?.size()");
        assertEquals(3, result.getInterpretResult());
        assertEquals(3, result.getCompileResult());
    }

    @Test
    public void testSafeContextCallOnList() {
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("list = null; &list ?:: contains(1)");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());

        result = FluxonTestUtil.runSilent("list = [1,2,3]; &list ?:: contains(1)");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    // ========== 复杂表达式测试 ==========

    @Test
    public void testComplexSafeAccessExpression() {
        FluxonTestUtil.TestResult result;

        // 复杂表达式：((null ?. method()) ?: default) ?:: process()
        result = FluxonTestUtil.runSilent(
                "obj = null; ((&obj?.toString()) ?: 'default') ?:: uppercase()");
        assertEquals("DEFAULT", result.getInterpretResult());
        assertEquals("DEFAULT", result.getCompileResult());

        result = FluxonTestUtil.runSilent(
                "obj = 'hello'; ((&obj?.toString()) ?: 'default') ?:: uppercase()");
        assertEquals("HELLO", result.getInterpretResult());
        assertEquals("HELLO", result.getCompileResult());
    }

    // ========== 边界情况测试 ==========

    @Test
    public void testSafeAccessOnOptionalReference() {
        // 可选引用 (&?) 与安全访问组合
        FluxonTestUtil.TestResult result;

        result = FluxonTestUtil.runSilent("&?unknownVar?.toString()");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());

        result = FluxonTestUtil.runSilent("&?unknownVar ?:: uppercase()");
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }

    @Test
    public void testMultipleSafeAccessChain() {
        // 多重安全访问链
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 'hello'; &a?.toUpperCase()?.toLowerCase()?.length()");
        assertEquals(5, result.getInterpretResult());
        assertEquals(5, result.getCompileResult());
    }
}
