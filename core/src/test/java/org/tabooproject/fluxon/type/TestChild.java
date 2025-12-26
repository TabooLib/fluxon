package org.tabooproject.fluxon.type;

/**
 * 继承测试用的子类
 *
 * @author sky
 */
public class TestChild extends TestParent {

    // ========== 子类自己的字段 ==========
    public String childField = "child-field-value";
    public int childInt = 200;

    // ========== 同名字段（隐藏父类字段）==========
    public String parentField = "child-shadows-parent";

    // ========== 子类自己的方法 ==========
    public String getChildName() {
        return "child-name";
    }

    public int getChildNumber() {
        return 888;
    }

    public String childMethod(String arg) {
        return "child:" + arg;
    }

    // ========== 重写父类方法 ==========
    @Override
    public String overridableMethod() {
        return "child-impl";
    }

    @Override
    public String overridableWithArg(String arg) {
        return "child:" + arg;
    }

    // ========== 添加重载 ==========
    public String overloadedInHierarchy(String value) {
        return "child-string:" + value;
    }

    public String overloadedInHierarchy(int a, int b) {
        return "child-two-ints:" + a + "," + b;
    }

    // ========== 调用父类方法 ==========
    public String callSuper() {
        return super.overridableMethod();
    }

    // ========== 访问父类字段 ==========
    public String getParentFieldViaSuper() {
        return super.parentField;
    }

    // ========== 静态方法（隐藏父类）==========
    public static String staticParentMethod() {
        return "static-child";
    }
}
