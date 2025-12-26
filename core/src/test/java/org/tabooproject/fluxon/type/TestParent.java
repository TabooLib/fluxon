package org.tabooproject.fluxon.type;

/**
 * 继承测试用的父类
 *
 * @author sky
 */
public class TestParent {

    // ========== 公共字段 ==========
    public String parentField = "parent-field-value";
    public int parentInt = 100;

    // ========== 受保护字段 ==========
    protected String protectedField = "protected-field-value";

    // ========== 私有字段 ==========
    private String privateParentField = "private-parent-value";

    // ========== 公共方法 ==========
    public String getParentName() {
        return "parent-name";
    }

    public int getParentNumber() {
        return 999;
    }

    public String parentMethod(String arg) {
        return "parent:" + arg;
    }

    // ========== 可被重写的方法 ==========
    public String overridableMethod() {
        return "parent-impl";
    }

    public String overridableWithArg(String arg) {
        return "parent:" + arg;
    }

    // ========== 重载方法（子类会添加更多重载）==========
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
}
