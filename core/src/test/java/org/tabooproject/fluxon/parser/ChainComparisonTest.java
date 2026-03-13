package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 链式比较运算符测试
 * 验证 a < b < c 脱糖为 a < b && b < c
 *
 * @author sky
 */
public class ChainComparisonTest {

    private Object eval(String source) {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        return Fluxon.eval(source, env);
    }

    @Test
    public void testChainLessThanTrue() {
        // 1 < 2 && 2 < 3
        assertEquals(true, eval("1 < 2 < 3"));
    }

    @Test
    public void testChainLessThanFalse() {
        // 1 < 3 为 true，但 3 < 2 为 false
        assertEquals(false, eval("1 < 3 < 2"));
    }

    @Test
    public void testThreeWayChain() {
        // 1 < 2 && 2 < 3 && 3 < 4
        assertEquals(true, eval("1 < 2 < 3 < 4"));
    }

    @Test
    public void testChainGreaterThanTrue() {
        // 5 > 3 && 3 > 1
        assertEquals(true, eval("5 > 3 > 1"));
    }

    @Test
    public void testChainGreaterThanFalse() {
        // 5 > 3 为 true，但 3 > 4 为 false
        assertEquals(false, eval("5 > 3 > 4"));
    }

    @Test
    public void testChainLessEqual() {
        // 1 <= 2 && 2 <= 3
        assertEquals(true, eval("1 <= 2 <= 3"));
    }

    @Test
    public void testChainMixedOperators() {
        // 1 < 2 && 2 >= 2
        assertEquals(true, eval("1 < 2 >= 2"));
    }

    @Test
    public void testSingleComparisonStillWorks() {
        assertEquals(true, eval("1 < 2"));
    }

    @Test
    public void testFourWayChain() {
        // 3 > 2 && 2 > 1 && 1 > 0
        assertEquals(true, eval("3 > 2 > 1 > 0"));
    }
}
