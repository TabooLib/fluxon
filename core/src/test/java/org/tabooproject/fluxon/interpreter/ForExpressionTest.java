package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 for 表达式的执行
 */
public class ForExpressionTest {

    /**
     * 测试基本的 for 循环
     */
    @Test
    public void testBasicForLoop() {
        // 遍历列表
        assertEquals(6, Fluxon.eval("result = 0; for i in [1, 2, 3] { result += &i }; &result"));
        // 遍历空列表
        assertEquals(0, Fluxon.eval("result = 0; for i in [] { result += 1 }; &result"));
        // 遍历范围
        assertEquals(10, Fluxon.eval("result = 0; for i in 1..4 { result += &i }; &result"));
    }
    
    /**
     * 测试嵌套 for 循环
     */
    @Test
    public void testNestedForLoop() {
        String script = 
            "result = 0; " +
            "for i in 1..3 { " +
            "  for j in 1..3 { " +
            "    result += (&i * &j); " +
            "  } " +
            "}; " +
            "&result";

        assertEquals(36, Fluxon.eval(script));
    }
    
    /**
     * 测试 for 循环中的解构
     */
    @Test
    public void testDestructuringInForLoop() {
        // 测试键值对解构
        String mapScript = 
            "map = [a: 1, b: 2, c: 3];" +
            "result = \"\"; " +
            "for (key, value) in &map { " +
            "  result = &result + &key + &value; " +
            "}; " +
            "&result";
            
        String mapResult = (String) Fluxon.eval(mapScript);
        assertTrue(mapResult.contains("a1"));
        assertTrue(mapResult.contains("b2"));
        assertTrue(mapResult.contains("c3"));
        
        // 测试列表解构
        String listScript = 
            "list = [[1, 2], [3, 4], [5, 6]]; " +
            "result = 0; " +
            "for (first, second) in &list { " +
            "  result += &first + &second; " +
            "}; " +
            "&result";
            
        assertEquals(21, Fluxon.eval(listScript));
    }
}