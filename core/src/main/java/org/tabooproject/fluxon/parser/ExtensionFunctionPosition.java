package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.OverloadSet;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Map;

public class ExtensionFunctionPosition {

    private final Map<Class<?>, OverloadSet> overloadSets;
    private final int index;

    public ExtensionFunctionPosition(Map<Class<?>, OverloadSet> overloadSets, int index) {
        this.overloadSets = overloadSets;
        this.index = index;
    }

    public Map<Class<?>, OverloadSet> getOverloadSets() {
        return overloadSets;
    }

    public int getIndex() {
        return index;
    }

    /**
     * 推断扩展函数的返回类型（统一类型检查）
     * 如果所有重载的返回类型一致则返回该类型，否则返回 null
     */
    public Type inferReturnType(Type[] argTypes) {
        if (overloadSets == null) return null;
        Type commonType = null;
        for (OverloadSet set : overloadSets.values()) {
            Function func = set.resolve(argTypes);
            if (func != null) {
                Type rt = func.getReturnType();
                if (commonType == null) {
                    commonType = rt;
                } else if (!commonType.equals(rt)) {
                    return null;
                }
            }
        }
        return commonType;
    }

    @Override
    public String toString() {
        return "ExtensionFunctionPosition{" +
                "overloadSets=" + overloadSets +
                ", index=" + index +
                '}';
    }
}
