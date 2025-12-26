package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.runtime.java.Export;

/**
 * 用于反射访问测试的Java对象类
 * 
 * @author sky
 */
public class TestObject {
    
    // ========== 公共字段 ==========
    public String publicField = "public-value";
    public int intField = 42;
    
    // ========== 私有字段 ==========
    private String privateField = "private-value";
    
    // ========== 嵌套对象 ==========
    public TestObject nested = null;
    
    // ========== 无参方法 ==========
    public String getName() {
        return "test-object";
    }
    
    public int getNumber() {
        return 100;
    }
    
    // ========== 有参方法 ==========
    public String concat(String a, String b) {
        return a + b;
    }
    
    public int add(int a, int b) {
        return a + b;
    }
    
    // ========== 重载方法 ==========
    public String process(String value) {
        return "string:" + value;
    }
    
    public String process(int value) {
        return "int:" + value;
    }
    
    public String process(String a, String b) {
        return "concat:" + a + b;
    }
    
    // ========== 链式调用支持 ==========
    public TestObject getSelf() {
        return this;
    }
    
    public TestObject createNested() {
        TestObject obj = new TestObject();
        obj.publicField = "nested-value";
        return obj;
    }
    
    // ========== 静态方法 ==========
    public static String staticMethod() {
        return "static-result";
    }
    
    // ========== 带@Export注解的方法（用于ClassBridge测试）==========
    
    /**
     * 带Export注解的方法，用于测试ClassBridge优先级
     * 当ClassBridge存在时，应该走bridge而不是反射
     */
    @Export
    public String bridgedMethod() {
        return "bridged-result";
    }
    
    @Export
    public String bridgedMethodWithArg(String arg) {
        return "bridged:" + arg;
    }
    
    @Export
    public int bridgedAdd(int a, int b) {
        return a + b + 1000; // 加1000以区分是否走了bridge
    }
}
