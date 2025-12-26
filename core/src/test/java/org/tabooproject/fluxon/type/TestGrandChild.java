package org.tabooproject.fluxon.type;

/**
 * 继承测试用的孙类（三层继承）
 *
 * @author sky
 */
public class TestGrandChild extends TestChild {

    public String grandChildField = "grandchild-field";

    public String getGrandChildName() {
        return "grandchild-name";
    }

    public String grandChildOnlyMethod() {
        return "grandchild-only";
    }

    @Override
    public String overridableMethod() {
        return "grandchild-override";
    }

    // 访问祖父类方法
    public String getFromGrandParent() {
        return getParentName();
    }

    // ========== 返回自身用于链式调用 ==========
    @Override
    public TestGrandChild getSelf() {
        return this;
    }
}
