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
    public int getParameterCount() {
        Function first = overloadSet.first();
        return first != null ? first.getParameterCount() : 0;
    }

    public OverloadSet getOverloadSet() {
        return overloadSet;
    }

    /**
     * 根据参数类型解析具体重载并返回其索引
     */
    public int resolveIndex(Type[] argTypes) {
        Function resolved = overloadSet.resolve(argTypes);
        if (resolved == null) {
            return baseIndex;
        }
        int offset = 0;
        for (Function f : overloadSet.getOverloads()) {
            if (f == resolved) {
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
