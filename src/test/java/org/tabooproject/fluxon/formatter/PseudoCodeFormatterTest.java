package org.tabooproject.fluxon.formatter;

import org.tabooproject.fluxon.util.PseudoCodeFormatter;

/**
 * PseudoCodeFormatter 测试类
 */
public class PseudoCodeFormatterTest {
    
    public static void main(String[] args) {
        // 测试各种伪代码格式化
        
        // 1. 测试简单语句
        testFormat("a = 1; b = 2; c = a + b;", "简单赋值语句");
        
        // 2. 测试条件语句
        testFormat("if x > 0 { y = x; } else { y = -x; }", "条件语句");
        
        // 3. 测试循环语句
        testFormat("while i < 10 { sum = sum + i; i = i + 1; }", "循环语句");
        
        // 4. 测试函数定义
        testFormat("def fibonacci(n) = if n <= 1 then n else fibonacci(n - 1) + fibonacci(n - 2)", "函数定义");
        
        // 5. 测试复杂表达式
        testFormat("result = a * (b + c) / (d - e) % f;", "复杂表达式");
        
        // 6. 测试复合语句
        testFormat("if condition { if nestedCondition { doSomething; } else { doSomethingElse; } } else { doAnotherThing; }", "嵌套条件语句");
    }
    
    private static void testFormat(String code, String description) {
        System.out.println("===== 测试: " + description + " =====");
        System.out.println("原始代码: " + code);
        System.out.println("格式化后: \n" + PseudoCodeFormatter.format(code));
        System.out.println();
    }
} 