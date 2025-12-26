package org.tabooproject.fluxon.type;

/**
 * 继承测试用的父类
 *
 * @author sky
 */
public class TestParent {

    // ========== 公共字段 ==========
    public String parentField = "parent-field";
    public int parentInt = 100;

    // ========== 受保护字段 ==========
    protected String protectedField = "protected-value";

    // ========== 私有字段 ==========
    private String privateParentField = "private-parent-value";

    // ========== 公共方法 ==========
    public String getParentName() {
        return "parent-name";
    }

    public int getParentNumber() {
        return 999;
    }

    public String parentMethod() {
        return "parent-method";
    }

    public String parentMethodWithArg(String arg) {
        return "parent:" + arg;
    }

    // ========== 可被重写的方法 ==========
    public String overridableMethod() {
        return "parent-overridable";
    }

    public String overridableWithArg(String arg) {
        return "parent:" + arg;
    }

    // ========== 重载方法（子类会添加更多重载）==========
    public String overloadedMethod() {
        return "parent-overload:0";
    }

    public String overloadedMethod(String arg) {
        return "parent-overload:1:" + arg;
    }

    public String overloadedInHierarchy(int value) {
        return "parent-int:" + value;
    }

    // ========== final 方法 ==========
    public final String finalMethod() {
        return "final-from-parent";
    }

    // ========== 静态方法 ==========
    public static String staticParentMethod() {
        return "static-parent";
    }

    // ========== 获取 protected 字段值 ==========
    public String getProtectedValue() {
        return protectedField;
    }

    // ========== 返回自身用于链式调用 ==========
    public TestParent getSelf() {
        return this;
    }

    // ========== concat 方法用于测试 ==========
    public String concat(String a, String b) {
        return a + b;
    }
}
