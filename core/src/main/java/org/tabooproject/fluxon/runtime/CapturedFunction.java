package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.parser.definition.Annotation;

import java.util.List;

/**
 * 绑定定义时环境的函数包装。
 * 捕获型 Lambda 逃逸后必须继续读取创建它的父环境，而不是调用点环境。
 */
public final class CapturedFunction implements Function {

    public static final Type TYPE = new Type(CapturedFunction.class);

    @NotNull
    private final Function delegate;
    @NotNull
    private final Environment capturedEnvironment;
    @Nullable
    private final CaptureFrame captureFrame;

    public CapturedFunction(@NotNull Function delegate, @NotNull Environment capturedEnvironment) {
        this(delegate, capturedEnvironment, null);
    }

    public CapturedFunction(@NotNull Function delegate, @NotNull Environment capturedEnvironment, @Nullable CaptureFrame captureFrame) {
        this.delegate = delegate;
        this.capturedEnvironment = capturedEnvironment;
        this.captureFrame = captureFrame;
    }

    @Nullable
    @Override
    public String getNamespace() {
        return delegate.getNamespace();
    }

    @NotNull
    @Override
    public String getName() {
        return delegate.getName();
    }

    @Nullable
    @Override
    public FunctionSignature getSignature() {
        return delegate.getSignature();
    }

    @Override
    public boolean isAsync() {
        return delegate.isAsync();
    }

    @Override
    public boolean isPrimarySync() {
        return delegate.isPrimarySync();
    }

    @Override
    public List<Annotation> getAnnotations() {
        return delegate.getAnnotations();
    }

    @Nullable
    @Override
    public DirectBinding getDirectBinding() {
        return delegate.getDirectBinding();
    }

    @Override
    public void call(@NotNull FunctionContext<?> context) {
        Environment previous = context.getEnvironment();
        CaptureFrame previousCaptureFrame = context.getCaptureFrame();
        context.setEnvironment(capturedEnvironment);
        context.setCaptureFrame(captureFrame);
        try {
            delegate.call(context);
        } finally {
            context.setEnvironment(previous);
            context.setCaptureFrame(previousCaptureFrame);
        }
    }
}
