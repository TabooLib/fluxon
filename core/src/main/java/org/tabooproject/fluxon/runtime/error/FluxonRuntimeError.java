package org.tabooproject.fluxon.runtime.error;

import org.tabooproject.fluxon.parser.SourceExcerpt;
import org.tabooproject.fluxon.runtime.Type;

/**
 * Fluxon 运行时错误基类
 * 所有运行时错误都应该继承此类
 */
public abstract class FluxonRuntimeError extends RuntimeException {

    public static final Type TYPE = new Type(FluxonRuntimeError.class);

    private transient SourceExcerpt sourceExcerpt;

    protected FluxonRuntimeError(String message) {
        super(message);
    }

    protected FluxonRuntimeError(String message, Throwable cause) {
        super(message, cause);
    }

    public FluxonRuntimeError attachSource(SourceExcerpt excerpt) {
        if (excerpt != null && this.sourceExcerpt == null) {
            this.sourceExcerpt = excerpt;
        }
        return this;
    }

    public SourceExcerpt getSourceExcerpt() {
        return sourceExcerpt;
    }

    @Override
    public String getMessage() {
        if (sourceExcerpt == null) {
            return super.getMessage();
        }
        return sourceExcerpt.formatDiagnostic(null, super.getMessage());
    }
}
