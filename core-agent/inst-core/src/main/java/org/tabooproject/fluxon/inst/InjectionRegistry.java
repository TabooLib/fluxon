package org.tabooproject.fluxon.inst;

import org.tabooproject.fluxon.inst.bytecode.BytecodeStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 注入注册表。
 * 管理所有注入规格的注册、撤销和查询。
 */
public class InjectionRegistry {

    private static final Logger LOGGER = Logger.getLogger(InjectionRegistry.class.getName());
    private static final InjectionRegistry INSTANCE = new InjectionRegistry();

    // 注入 ID -> InjectionSpec
    private final Map<String, InjectionSpec> specsById = new ConcurrentHashMap<>();
    // 类名（内部格式）-> 该类的所有注入规格
    private final Map<String, List<InjectionSpec>> specsByClass = new ConcurrentHashMap<>();

    private InjectionRegistry() {
    }

    public static InjectionRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 注册注入规格。
     *
     * @param spec 注入规格
     * @return 注入 ID
     */
    public String register(InjectionSpec spec) {
        String className = spec.getClassName();
        specsById.put(spec.getId(), spec);
        specsByClass.computeIfAbsent(className, k -> new ArrayList<>()).add(spec);
        // REPLACE 类型冲突警告
        if (spec.getType() == InjectionType.REPLACE) {
            List<InjectionSpec> classSpecs = specsByClass.get(className);
            for (InjectionSpec existing : classSpecs) {
                if (existing != spec && existing.getType() == InjectionType.REPLACE && existing.matchesMethod(spec.getMethodName(), spec.getMethodDescriptor())) {
                    LOGGER.warning("REPLACE 注入冲突: " + existing.getTarget() + " 将被覆盖");
                }
            }
        }
        triggerRetransform(className);
        return spec.getId();
    }

    /**
     * 通过 ID 撤销注入。
     */
    public boolean unregister(String id) {
        InjectionSpec spec = specsById.remove(id);
        if (spec == null) {
            return false;
        }
        String className = spec.getClassName();
        List<InjectionSpec> classSpecs = specsByClass.get(className);
        if (classSpecs != null) {
            classSpecs.remove(spec);
            if (classSpecs.isEmpty()) {
                specsByClass.remove(className);
                BytecodeStore.getInstance().remove(className);
            }
        }
        triggerRetransform(className);
        return true;
    }

    /**
     * 通过目标撤销匹配的注入。
     */
    public boolean unregisterByTarget(String className, String methodName, String descriptor) {
        String internalClassName = className.replace('.', '/');
        List<InjectionSpec> classSpecs = specsByClass.get(internalClassName);
        if (classSpecs == null || classSpecs.isEmpty()) {
            return false;
        }
        List<InjectionSpec> toRemove = new ArrayList<>();
        for (InjectionSpec spec : classSpecs) {
            if (spec.matchesMethod(methodName, descriptor)) {
                toRemove.add(spec);
            }
        }
        if (toRemove.isEmpty()) {
            return false;
        }
        for (InjectionSpec spec : toRemove) {
            specsById.remove(spec.getId());
            classSpecs.remove(spec);
            CallbackDispatcher.unregister(spec.getId());
        }
        if (classSpecs.isEmpty()) {
            specsByClass.remove(internalClassName);
            BytecodeStore.getInstance().remove(internalClassName);
        }
        triggerRetransform(internalClassName);
        return true;
    }

    /**
     * 获取指定类的所有注入规格。
     */
    public List<InjectionSpec> getSpecsForClass(String internalClassName) {
        List<InjectionSpec> specs = specsByClass.get(internalClassName);
        return specs != null ? new ArrayList<>(specs) : Collections.<InjectionSpec>emptyList();
    }

    /**
     * 获取所有注入规格。
     */
    public Collection<InjectionSpec> getAllSpecs() {
        return new ArrayList<>(specsById.values());
    }

    /**
     * 检查指定类是否有任何注入。
     */
    public boolean hasInjectionsForClass(String internalClassName) {
        List<InjectionSpec> specs = specsByClass.get(internalClassName);
        return specs != null && !specs.isEmpty();
    }

    /**
     * 清除所有注入。
     */
    public void clear() {
        Set<String> classNames = new HashSet<>(specsByClass.keySet());
        specsById.clear();
        specsByClass.clear();
        BytecodeStore.getInstance().clear();
        CallbackDispatcher.clearAll();
        for (String className : classNames) {
            triggerRetransform(className);
        }
    }

    private void triggerRetransform(String className) {
        if (!InstrumentationBridge.isAvailable()) {
            LOGGER.fine("Instrumentation 不可用，将在类首次加载时应用注入");
            return;
        }
        try {
            String dotClassName = className.replace('/', '.');
            Class<?> clazz = Class.forName(dotClassName);
            InstrumentationBridge.retransformClasses(clazz);
        } catch (ClassNotFoundException e) {
            LOGGER.fine("目标类尚未加载: " + className);
        } catch (Exception e) {
            LOGGER.warning("重新转换类失败: " + className + ", 错误: " + e.getMessage());
        }
    }
}
