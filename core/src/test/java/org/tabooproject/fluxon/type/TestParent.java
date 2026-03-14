package org.tabooproject.fluxon.type;

/**
 * 继承测试用的父类
 *
 * @author sky
 */
public class TestParent {

    public String parentField = "parent-field";
    public int parentInt = 100;

    protected String protectedField = "protected-value";

    private String privateParentField = "private-parent-value";

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

    public String overridableMethod() {
        return "parent-overridable";
    }

    public String overridableWithArg(String arg) {
        return "parent:" + arg;
    }

    public String overloadedMethod() {
        return "parent-overload:0";
    }

    public String overloadedMethod(String arg) {
        return "parent-overload:1:" + arg;
    }

    public String overloadedInHierarchy(int value) {
        return "parent-int:" + value;
    }

    public final String finalMethod() {
        return "final-from-parent";
    }

    public static String staticParentMethod() {
        return "static-parent";
    }

    public String getProtectedValue() {
        return protectedField;
    }

    public TestParent getSelf() {
        return this;
    }

    public String concat(String a, String b) {
        return a + b;
    }
}
