package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lambda 定义时捕获帧。
 * 帧内只保存 cell 引用，变量读写仍落在共享 cell 上。
 */
public final class CaptureFrame {

    public static final Type TYPE = new Type(CaptureFrame.class);

    @NotNull
    private final CaptureCell[] cells;

    public CaptureFrame(int size) {
        this.cells = new CaptureCell[size];
    }

    public void setCell(int index, @NotNull CaptureCell cell) {
        cells[index] = cell;
    }

    @Nullable
    public CaptureCell getCell(int index) {
        if (index < 0 || index >= cells.length) return null;
        return cells[index];
    }

    public int size() {
        return cells.length;
    }

    @Nullable
    public Object get(int index) {
        CaptureCell cell = cells[index];
        return cell.get();
    }

    public void set(int index, @Nullable Object value) {
        CaptureCell cell = cells[index];
        cell.set(value);
    }
}
