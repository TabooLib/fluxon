package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.Nullable;

/**
 * 闭包捕获槽位。
 * 父函数和逃逸 Lambda 共享同一个 cell，保证后续赋值仍能被捕获方观察到。
 */
public final class CaptureCell {

    public static final Type TYPE = new Type(CaptureCell.class);

    @Nullable
    private Object value;

    public CaptureCell(@Nullable Object value) {
        this.value = value;
    }

    @Nullable
    public Object get() {
        return value;
    }

    public void set(@Nullable Object value) {
        this.value = value;
    }
}
