package org.tabooproject.fluxon.inst.agent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 端的注入注册表。
 * 存储简单的注入信息，避免类加载器隔离问题。
 */
public class AgentRegistry {

    public static final AgentRegistry INSTANCE = new AgentRegistry();

    // className -> List<[id, methodName, descriptor, type]>
    private final Map<String, List<String[]>> specsByClass = new ConcurrentHashMap<>();

    private AgentRegistry() {
    }

    /**
     * 注册注入（由 Plugin 通过反射调用）
     */
    public void register(String className, String id, String methodName, String descriptor, String type) {
        specsByClass.computeIfAbsent(className, k -> new ArrayList<>()).add(new String[]{id, methodName, descriptor, type});
    }

    /**
     * 撤销注入
     */
    public void unregister(String className, String id) {
        List<String[]> specs = specsByClass.get(className);
        if (specs != null) {
            specs.removeIf(s -> s[0].equals(id));
            if (specs.isEmpty()) {
                specsByClass.remove(className);
            }
        }
    }

    /**
     * 获取类的所有注入规格
     */
    public List<String[]> getSpecsForClass(String className) {
        List<String[]> specs = specsByClass.get(className);
        return specs != null ? new ArrayList<>(specs) : Collections.<String[]>emptyList();
    }

    /**
     * 检查类是否有注入
     */
    public boolean hasInjectionsForClass(String className) {
        List<String[]> specs = specsByClass.get(className);
        return specs != null && !specs.isEmpty();
    }

    public void clear() {
        specsByClass.clear();
    }
}
