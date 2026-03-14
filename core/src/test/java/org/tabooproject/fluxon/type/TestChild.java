package org.tabooproject.fluxon.type;

/**
 * 继承测试用的子类
 *
 * @author sky
 */
public class TestChild extends TestParent {

    public String childField = "child-field";
    public int childInt = 200;

    public String getChildName() {
        return "child-name";
    }

    public int getChildNumber() {
        return 888;
    }

    public String childMethod(String arg) {
        return "child:" + arg;
    }

    public String childOnlyMethod() {
        return "child-only";
    }

    @Override
    public String overridableMethod() {
        return "child-override";
    }

    @Override
    public String overridableWithArg(String arg) {
        return "child:" + arg;
    }

    public String overloadedMethod(String a, String b) {
        return "child-overload:2:" + a + ":" + b;
    }

    public String overloadedInHierarchy(String value) {
        return "child-string:" + value;
    }

    public String overloadedInHierarchy(int a, int b) {
        return "child-two-ints:" + a + "," + b;
    }

    public String callSuperMethod() {
        return "super:" + super.overridableMethod();
    }

    public String getParentFieldViaSuper() {
        return super.parentField;
    }

    @Override
    public TestChild getSelf() {
        return this;
    }

    public static String staticParentMethod() {
        return "static-child";
    }
}
