package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.type.TestObject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CallSite 缓存测试
 * 测试编译模式下 invokedynamic 的 CallSite 缓存行为
 *
 * @author sky
 */
public class CallSiteCachingTest extends MemberAccessTestBase {

    // ========== 同一方法多次调用 ==========

    @Test
    public void testRepeatedMethodCall() throws Exception {
        // 同一方法多次调用应该复用 CallSite
        String source = 
            "a = &obj.getName(); " +
            "b = &obj.getName(); " +
            "c = &obj.getName(); " +
            "&a + &b + &c";
        Object result = compile(source);
        assertEquals("test-objecttest-objecttest-object", result);
    }

    @Test
    public void testRepeatedFieldAccess() throws Exception {
        // 同一字段多次访问
        String source = 
            "a = &obj.intField; " +
            "b = &obj.intField; " +
            "c = &obj.intField; " +
            "&a + &b + &c";
        Object result = compile(source);
        assertEquals(126, result); // 42 * 3
    }

    // ========== 循环中的 CallSite 缓存 ==========

    @Test
    public void testMethodCallInLoop() throws Exception {
        // 循环中多次调用同一方法
        String source = 
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 10) { " +
            "  sum = &sum + &obj.add(1, 2); " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        Object result = compile(source);
        assertEquals(30, result); // 3 * 10
    }

    @Test
    public void testFieldAccessInLoop() throws Exception {
        // 循环中多次访问同一字段
        String source = 
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 5) { " +
            "  sum = &sum + &obj.intField; " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        Object result = compile(source);
        assertEquals(210, result); // 42 * 5
    }

    @Test
    public void testChainedCallInLoop() throws Exception {
        // 循环中的链式调用
        String source = 
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 3) { " +
            "  sum = &sum + &obj.getSelf().intField; " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        Object result = compile(source);
        assertEquals(126, result); // 42 * 3
    }

    // ========== 多个不同的 CallSite ==========

    @Test
    public void testMultipleDistinctCallSites() throws Exception {
        // 多个不同的方法调用，每个有自己的 CallSite
        String source = 
            "a = &obj.getName(); " +        // CallSite 1
            "b = &obj.getNumber(); " +      // CallSite 2
            "c = &obj.intField; " +         // CallSite 3
            "d = &obj.publicField; " +      // CallSite 4
            "&c + &b";                      // 42 + 100
        Object result = compile(source);
        assertEquals(142, result);
    }

    @Test
    public void testMultipleMethodsWithArgs() throws Exception {
        // 多个带参数的方法调用
        String source = 
            "a = &obj.add(1, 2); " +           // CallSite 1
            "b = &obj.add(10, 20); " +         // CallSite 2 (同名但不同参数值)
            "c = &obj.concat('x', 'y'); " +    // CallSite 3
            "&a + &b";                         // 3 + 30
        Object result = compile(source);
        assertEquals(33, result);
    }

    // ========== 解释器与编译器一致性 ==========

    @Test
    public void testCachingConsistencySimple() throws Exception {
        String source = "&obj.getName() + &obj.getName()";
        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
    }

    @Test
    public void testCachingConsistencyLoop() throws Exception {
        String source = 
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 5) { " +
            "  sum = &sum + &obj.add(2, 3); " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
        assertEquals(25, interpretResult); // 5 * 5
    }

    // ========== 递归场景 ==========

    @Test
    public void testNestedLoopCaching() throws Exception {
        String source = 
            "total = 0; " +
            "i = 0; " +
            "while (&i < 3) { " +
            "  j = 0; " +
            "  while (&j < 2) { " +
            "    total = &total + &obj.intField; " +
            "    j = &j + 1; " +
            "  }; " +
            "  i = &i + 1; " +
            "}; " +
            "&total";
        Object result = compile(source);
        assertEquals(252, result); // 42 * 3 * 2
    }

    // ========== 条件分支中的 CallSite ==========

    @Test
    public void testCallSiteInBranches() throws Exception {
        String source = 
            "result = 0; " +
            "if &obj.booleanField { " +
            "  result = &obj.intField; " +      // 这个 CallSite
            "} else { " +
            "  result = &obj.getNumber(); " +   // 和这个是不同的
            "}; " +
            "&result";
        Object result = compile(source);
        assertEquals(42, result);
    }

    @Test
    public void testSameCallSiteInBothBranches() throws Exception {
        String source = 
            "result = 0; " +
            "if &obj.booleanField { " +
            "  result = &obj.intField + 1; " +
            "} else { " +
            "  result = &obj.intField + 2; " +
            "}; " +
            "&result";
        Object result = compile(source);
        assertEquals(43, result); // 42 + 1
    }
}
