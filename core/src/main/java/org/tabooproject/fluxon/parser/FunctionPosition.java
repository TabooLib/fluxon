package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionSignature;
import org.tabooproject.fluxon.runtime.OverloadSet;
import org.tabooproject.fluxon.runtime.Type;

public class FunctionPosition implements Callable {

    private final OverloadSet overloadSet;
    private final int baseIndex;

    public FunctionPosition(OverloadSet overloadSet, int baseIndex) {
        this.overloadSet = overloadSet;
        this.baseIndex = baseIndex;
    }

    @Override
    public FunctionSignature getSignature() {
        Function first = overloadSet.first();
        return first != null ? first.getSignature() : null;
    }

    public OverloadSet getOverloadSet() {
        return overloadSet;
    }

    /**
     * 根据参数类型解析具体重载并返回其索引
     */
    public int resolveIndex(Type[] argTypes) {
        int offset = 0;
        for (Function f : overloadSet.getOverloads()) {
            if (matchesSignature(f, argTypes)) {
                return baseIndex + offset;
            }
            offset++;
        }
        return baseIndex;
    }

    /**
     * 根据参数类型解析具体函数
     */
    public Function resolve(Type[] argTypes) {
        Function resolved = overloadSet.resolve(argTypes);
        return resolved != null ? resolved : overloadSet.first();
    }

    private boolean matchesSignature(Function f, Type[] argTypes) {
        FunctionSignature sig = f.getSignature();
        if (sig == null) return true;
        if (sig.getParameterCount() != argTypes.length) return false;
        return true;
    }

    public int getBaseIndex() {
        return baseIndex;
    }

    @Override
    public String toString() {
        return "FunctionPosition{" +
                "overloadSet=" + overloadSet +
                ", baseIndex=" + baseIndex +
                '}';
    }
}
