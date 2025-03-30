package org.tabooproject.fluxon.jsr223;

import javax.script.*;

/**
 * FluxonScriptEngine 测试类
 */
public class FluxonScriptEngineTest {
    
    public static void main(String[] args) {
        try {
            // 获取脚本引擎管理器并尝试加载 Fluxon 引擎
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("fluxon");

            // 如果无法通过管理器获取，则尝试直接创建引擎实例
            if (engine == null) {
                System.out.println("Failed to get Fluxon script engine, please check service provider configuration.");
                System.out.println("Trying to create engine directly...");
                engine = new FluxonScriptEngine(new FluxonScriptEngineFactory());
            }
            
            System.out.println("Fluxon Script Engine: " + engine.getFactory().getEngineName() + " " + engine.getFactory().getEngineVersion());
            
            // 测试基本的算术表达式
            System.out.println("\nTesting basic expressions:");
            Object result = engine.eval("1 + 2 * 3");
            System.out.println("1 + 2 * 3 = " + result);
            
            // 测试将 Java 变量绑定到脚本环境
            System.out.println("\nTesting variable bindings:");
            engine.put("x", 10);
            engine.put("y", 20);
            result = engine.eval("&x + &y");
            System.out.println("x + y = " + result);
            
            // 测试在脚本中定义变量并在 Java 中读取
            System.out.println("\nTesting script-defined variables:");
            engine.eval("z = &x * &y");
            System.out.println("z = " + engine.get("z"));
            
            // 测试函数定义和递归调用
            System.out.println("\nTesting function definition and recursion:");
            engine.eval("def factorial(n) = {\n" +
                        "    if &n <= 1 {\n" +
                        "         1 \n" +
                        "    } else {\n" +
                        "         &n * factorial(&n - 1) \n" +
                        "    };\n" +
                        "}");
            
            engine.put("n", 5);
            result = engine.eval("factorial(&n)");
            System.out.println("factorial(5) = " + result);
            
            // 测试自定义绑定对象
            System.out.println("\nTesting custom bindings:");
            Bindings bindings = new FluxonBindings();
            bindings.put("a", 30);
            bindings.put("b", 40);
            result = engine.eval("&a * &b", bindings);
            System.out.println("a * b = " + result);
            
            // 测试从脚本向绑定对象传递变量
            engine.eval("c = &a + &b", bindings);
            System.out.println("c = " + bindings.get("c"));
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 