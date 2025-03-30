package org.tabooproject.fluxon.jsr223;

import org.jetbrains.annotations.NotNull;

import javax.script.Bindings;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fluxon 绑定实现
 * 用于管理脚本变量
 */
public class FluxonBindings implements Bindings {

    private final Map<String, Object> bindings = new HashMap<>();

    @Override
    public Object put(String name, Object value) {
        return bindings.put(name, value);
    }

    @Override
    public void putAll(Map<? extends String, ?> toMerge) {
        bindings.putAll(toMerge);
    }

    @Override
    public boolean containsKey(Object key) {
        return bindings.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        return bindings.get(key);
    }

    @Override
    public Object remove(Object key) {
        return bindings.remove(key);
    }

    @Override
    public int size() {
        return bindings.size();
    }

    @Override
    public boolean isEmpty() {
        return bindings.isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        return bindings.containsValue(value);
    }

    @Override
    public void clear() {
        bindings.clear();
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return bindings.keySet();
    }

    @NotNull
    @Override
    public Collection<Object> values() {
        return bindings.values();
    }

    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return bindings.entrySet();
    }
} 