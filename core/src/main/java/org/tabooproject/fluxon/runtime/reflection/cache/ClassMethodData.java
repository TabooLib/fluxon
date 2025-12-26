package org.tabooproject.fluxon.runtime.reflection.cache;

import org.tabooproject.fluxon.runtime.reflection.resolve.MethodResolver;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每个类的方法缓存数据
 */
public final class ClassMethodData {

    final ConcurrentHashMap<String, ArityCache> cache = new ConcurrentHashMap<>();
    final Map<String, List<Method>> index;

    public ClassMethodData(Class<?> clazz) {
        this.index = MethodResolver.buildMethodIndex(clazz);
    }
}