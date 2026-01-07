package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.parser.error.VariableNotFoundException;
import org.tabooproject.fluxon.runtime.Environment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 下划线前缀局部变量测试
 * 测试 _ 前缀变量强制使用 localVariables 数组存储的特性
 */
public class UnderscoreLocalVariableTest {

    /**
     * 测试根层级 _ 前缀变量的基本赋值和引用
     */
    @Test
    public void testRootLevelUnderscoreVariableBasic() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("_temp = 100; &_temp");
        FluxonTestUtil.assertBothEqual(100, result);
    }

    /**
     * 测试根层级多个 _ 前缀变量
     */
    @Test
    public void testRootLevelMultipleUnderscoreVariables() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("_a = 1; _b = 2; _c = 3; &_a + &_b + &_c");
        FluxonTestUtil.assertBothEqual(6, result);
    }

    /**
     * 测试 _ 前缀变量的复合赋值操作
     */
    @Test
    public void testUnderscoreVariableCompoundAssignment() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("_count = 10; _count += 5; &_count");
        FluxonTestUtil.assertBothEqual(15, result);
    }

    /**
     * 测试函数内无法访问根层级 _ 前缀变量（应报错）
     */
    @Test
    public void testCannotAccessRootUnderscoreVariableInFunction() {
        String source = "_rootLocal = 100; def foo() = &_rootLocal; foo()";
        assertThrows(VariableNotFoundException.class, () -> {
            FluxonTestUtil.runSilent(source);
        });
    }

    /**
     * 测试混合使用普通根变量和 _ 前缀变量
     */
    @Test
    public void testMixedRootAndUnderscoreVariables() {
        String source =
            "global = 100\n" +
            "_local = 200\n" +
            "def foo() = &global\n" +
            "foo() + &_local";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(source);
        FluxonTestUtil.assertBothEqual(300, result);
    }

    /**
     * 测试普通根变量可以跨函数访问
     */
    @Test
    public void testGlobalVariableCrossFunctionAccess() {
        String source =
            "global = 50\n" +
            "def increment() { global = &global + 1 }\n" +
            "increment()\n" +
            "increment()\n" +
            "&global";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(source);
        FluxonTestUtil.assertBothEqual(52, result);
    }

    /**
     * 测试函数内部的 _ 前缀变量正常工作
     */
    @Test
    public void testUnderscoreVariableInsideFunction() {
        String source =
            "def sum(a, b) {\n" +
            "    _result = &a + &b\n" +
            "    &_result\n" +
            "}\n" +
            "sum(10, 20)";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(source);
        FluxonTestUtil.assertBothEqual(30, result);
    }

    /**
     * 测试根层级 _ 前缀变量在循环中的使用
     */
    @Test
    public void testUnderscoreVariableInLoop() {
        String source =
            "_sum = 0\n" +
            "for i in [1, 2, 3, 4] {\n" +
            "    _sum = &_sum + &i\n" +
            "}\n" +
            "&_sum";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(source);
        FluxonTestUtil.assertBothEqual(10, result);
    }

    /**
     * 测试 ParsedScript API
     */
    @Test
    public void testParsedScriptApi() {
        ParsedScript script = Fluxon.parse("_x = 10; _y = 20; &_x + &_y");
        
        // 验证 rootLocalVariableCount
        assertEquals(2, script.getRootLocalVariableCount());
        
        // 使用 newEnvironment 创建的环境执行
        Object result = script.eval();
        assertEquals(30, result);
    }

    /**
     * 测试 ParsedScript 复用执行
     */
    @Test
    public void testParsedScriptReuse() {
        ParsedScript script = Fluxon.parse("_counter = 0; _counter += 1; &_counter");
        
        // 每次 eval() 创建新环境，结果应该相同
        assertEquals(1, script.eval());
        assertEquals(1, script.eval());
    }

    /**
     * 测试 ParsedScript 使用自定义环境
     */
    @Test
    public void testParsedScriptWithCustomEnvironment() {
        ParsedScript script = Fluxon.parse("_x = 100; &_x");
        
        Environment env = script.newEnvironment();
        assertEquals(100, script.eval(env));
    }
}
