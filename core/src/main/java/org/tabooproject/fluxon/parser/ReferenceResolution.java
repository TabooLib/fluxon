package org.tabooproject.fluxon.parser;

/**
 * 变量引用解析结果
 */
public class ReferenceResolution {

    public enum Kind {
        LOCAL,
        ROOT,
        UNDEFINED
    }

    private final Kind kind;
    private final int index;

    private ReferenceResolution(Kind kind, int index) {
        this.kind = kind;
        this.index = index;
    }

    public static ReferenceResolution local(int index) {
        return new ReferenceResolution(Kind.LOCAL, index);
    }

    public static ReferenceResolution root() {
        return new ReferenceResolution(Kind.ROOT, -1);
    }

    public static ReferenceResolution undefined() {
        return new ReferenceResolution(Kind.UNDEFINED, -1);
    }

    public Kind getKind() {
        return kind;
    }

    public int getIndex() {
        return index;
    }

    public boolean isFound() {
        return kind != Kind.UNDEFINED;
    }
}
