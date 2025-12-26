package org.tabooproject.fluxon.type;

/**
 * 继承测试用的孙类（三层继承）
 *
 * @author sky
 */
public class TestGrandChild extends TestChild {

    public String grandChildField = "grandchild-field-value";

    public String getGrandChildName() {
        return "grandchild-name";
    }

    @Override
    public String overridableMethod() {
        return "grandchild-impl";
    }

    // 访问祖父类方法
    public String getFromGrandParent() {
        return getParentName();
    }
}
