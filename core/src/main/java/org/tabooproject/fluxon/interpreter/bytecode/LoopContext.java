package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.Label;

/**
 * 循环上下文，包含 break 和 continue 的跳转标签
 */
public class LoopContext {
    private final Label breakLabel;
    private final Label continueLabel;

    public LoopContext(Label breakLabel, Label continueLabel) {
        this.breakLabel = breakLabel;
        this.continueLabel = continueLabel;
    }

    public Label getBreakLabel() {
        return breakLabel;
    }

    public Label getContinueLabel() {
        return continueLabel;
    }
}
