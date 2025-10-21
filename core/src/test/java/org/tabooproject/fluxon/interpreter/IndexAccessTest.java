package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.FluxonFeatures;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 索引访问功能测试
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class IndexAccessTest {

    @BeforeEach
    public void BeforeEach() {
        FluxonFeatures.DEFAULT_ALLOW_KETHER_STYLE_CALL = true;
    }

    @Test
    public void testListIndexAccess() {
        Object result = Fluxon.eval("list = [10, 20, 30]\n" +
                "&list[0]");
        assertEquals(10, result);
        result = Fluxon.eval("list = [10, 20, 30]\n" +
                "&list[1]");
        assertEquals(20, result);
        result = Fluxon.eval("list = [10, 20, 30]\n" +
                "&list[2]");
        assertEquals(30, result);
    }

    @Test
    public void testMapIndexAccess() {
        Object result = Fluxon.eval("map = [name: 'test', value: 42]\n" +
                "&map['name']");
        assertEquals("test", result);
        result = Fluxon.eval("map = [name: 'test', value: 42]\n" +
                "&map['value']");
        assertEquals(42, result);
    }

    @Test
    public void testListIndexAssignment() {
        Object result = Fluxon.eval("list = [1, 2, 3]\n" +
                "&list[0] = 99\n" +
                "&list[0]");
        assertEquals(99, result);
    }

    @Test
    public void testMapIndexAssignment() {
        Object result = Fluxon.eval("map = [key: 'old']\n" +
                "&map['key'] = 'new'\n" +
                "&map['key']");
        assertEquals("new", result);
    }

    @Test
    public void testCompoundIndexAssignment() {
        Object result = Fluxon.eval("list = [10, 20, 30]\n" +
                "&list[0] += 5\n" +
                "&list[0]");
        assertEquals(15, result);
    }

    @Test
    public void testMultiIndexAccess() {
        Object result = Fluxon.eval("nested = [a: [b: [c: 100]]]\n" +
                "&nested['a', 'b', 'c']");
        assertEquals(100, result);
    }

    @Test
    public void testChainedIndexAccess() {
        Object result = Fluxon.eval("nested = [a: [b: [c: 200]]]\n" +
                "&nested['a']['b']['c']");
        assertEquals(200, result);
    }

    @Test
    public void testReferenceIndexAccess() {
        Object result = Fluxon.eval("map = [:]\n" +
                "&map['test'] = 42\n" +
                "&map['test']");
        assertEquals(42, result);
    }

    @Test
    public void testReferenceIndexCompoundAssignment() {
        Object result = Fluxon.eval("list = [5, 10, 15]\n" +
                "&list[1] *= 2\n" +
                "&list[1]");
        assertEquals(20, result);
    }
}
