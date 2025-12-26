package org.tabooproject.fluxon.runtime.reflection.cache;

import org.tabooproject.fluxon.runtime.reflection.resolve.ConstructorResolver;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * 每个类的构造函数缓存数据
 */
public final class ClassConstructorData {

    final ArityCache cache = new ArityCache();
    final List<Constructor<?>> index;

    public ClassConstructorData(Class<?> clazz) {
        this.index = ConstructorResolver.buildConstructorIndex(clazz);
    }
}
