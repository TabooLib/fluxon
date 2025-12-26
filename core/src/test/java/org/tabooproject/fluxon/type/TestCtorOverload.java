package org.tabooproject.fluxon.type;

public class TestCtorOverload {

    public final String tag;

    public TestCtorOverload(String value) {
        this.tag = "string";
    }

    public TestCtorOverload(Object value) {
        this.tag = "object";
    }
}
