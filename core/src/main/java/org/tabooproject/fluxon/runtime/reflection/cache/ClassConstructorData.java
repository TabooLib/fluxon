package org.tabooproject.fluxon.runtime.reflection.cache;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 每个类的构造函数缓存数据
 */
public final class ClassConstructorData {

    final ArityCache cache = new ArityCache();
    final List<Constructor<?>> constructors;

    public ClassConstructorData(Class<?> clazz) {
        this.constructors = new ArrayList<>(Arrays.asList(clazz.getConstructors()));
    }
}
