package org.tabooproject.fluxon.inst;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * 运行时字节码注入注册表。
 * 
 * <p>负责管理所有注入规范（InjectionSpec）的生命周期，并与 Java Agent 端的 AgentRegistry 保持同步。
 * 主要功能包括：
 * <ul>
 *   <li>注册和注销注入规范</li>
 *   <li>维护注入规范的两级索引（按 ID 和按类名）</li>
 *   <li>通过反射与 Agent 端注册表双向同步</li>
 *   <li>触发已加载类的重新转换（retransform）以应用字节码修改</li>
 * </ul>
 * 
 * <p>线程安全：使用 ConcurrentHashMap 和 volatile 保证并发安全。
 * 
 * @see InjectionSpec
 * @see InstrumentationBridge
 */
public class InjectionRegistry {

    /** Agent 端注册表的全限定类名 */
    private static final String AGENT_REGISTRY = "org.tabooproject.fluxon.inst.agent.AgentRegistry";
    
    /** 日志记录器 */
    private static final Logger LOGGER = Logger.getLogger(InjectionRegistry.class.getName());
    
    /** 单例实例 */
    private static final InjectionRegistry INSTANCE = new InjectionRegistry();

    /** 按唯一 ID 索引的注入规范映射 */
    private final Map<String, InjectionSpec> specsById = new ConcurrentHashMap<>();
    
    /** 按类名（内部格式）索引的注入规范列表映射，一个类可有多个注入点 */
    private final Map<String, List<InjectionSpec>> specsByClass = new ConcurrentHashMap<>();
    
    /** Agent 端注册表实例（通过反射获取） */
    private static volatile Object agentRegistry;
    
    /** Agent 端的注册方法（register） */
    private static volatile Method registerMethod;
    
    /** Agent 端的注销方法（unregister） */
    private static volatile Method unregisterMethod;
    
    /** Agent 端注册表是否已初始化 */
    private static volatile boolean agentInitialized = false;

    /**
     * 私有构造函数，防止外部实例化。
     */
    private InjectionRegistry() {
    }

    /**
     * 获取单例实例。
     * 
     * @return 注入注册表实例
     */
    public static InjectionRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 注册一个注入规范。
     * 
     * <p>注册流程：
     * <ol>
     *   <li>将规范加入内部索引（按 ID 和类名）</li>
     *   <li>同步到 Agent 端注册表</li>
     *   <li>触发目标类的重新转换（如已加载）</li>
     * </ol>
     * 
     * @param spec 注入规范
     * @return 注入规范的唯一 ID
     */
    public String register(InjectionSpec spec) {
        String className = spec.getClassName();
        specsById.put(spec.getId(), spec);
        specsByClass.computeIfAbsent(className, k -> new CopyOnWriteArrayList<>()).add(spec);
        syncToAgent(className, spec);
        triggerRetransform(className);
        return spec.getId();
    }

    /**
     * 注销指定 ID 的注入规范。
     * 
     * <p>注销流程：
     * <ol>
     *   <li>从内部索引中移除</li>
     *   <li>从 Agent 端注册表移除</li>
     *   <li>触发目标类的重新转换以恢复原始字节码</li>
     * </ol>
     * 
     * @param id 注入规范的唯一 ID
     * @return 是否成功注销（false 表示未找到对应 ID）
     */
    public boolean unregister(String id) {
        InjectionSpec spec = specsById.remove(id);
        if (spec == null) return false;
        String className = spec.getClassName();
        List<InjectionSpec> classSpecs = specsByClass.get(className);
        if (classSpecs != null) {
            classSpecs.remove(spec);
            if (classSpecs.isEmpty()) {
                specsByClass.remove(className);
            }
        }
        unsyncFromAgent(className, id);
        CallbackDispatcher.unregister(id);
        triggerRetransform(className);
        return true;
    }

    /**
     * 按目标方法注销注入规范。
     * 
     * <p>批量移除指定类中匹配方法签名的所有注入点，用于清理特定方法的所有拦截器。
     * 
     * @param className 目标类名（点分隔格式，如 "com.example.Foo"）
     * @param methodName 目标方法名
     * @param descriptor 方法描述符（JVM 格式，如 "(Ljava/lang/String;)V"）
     * @return 是否成功注销至少一个注入点
     */
    public boolean unregisterByTarget(String className, String methodName, String descriptor) {
        String internalClassName = className.replace('.', '/');
        List<InjectionSpec> classSpecs = specsByClass.get(internalClassName);
        if (classSpecs == null || classSpecs.isEmpty()) return false;
        List<InjectionSpec> toRemove = new ArrayList<>();
        for (InjectionSpec spec : classSpecs) {
            if (spec.matchesMethod(methodName, descriptor)) {
                toRemove.add(spec);
            }
        }
        if (toRemove.isEmpty()) return false;
        for (InjectionSpec spec : toRemove) {
            specsById.remove(spec.getId());
            classSpecs.remove(spec);
            unsyncFromAgent(internalClassName, spec.getId());
            CallbackDispatcher.unregister(spec.getId());
        }
        if (classSpecs.isEmpty()) {
            specsByClass.remove(internalClassName);
        }
        triggerRetransform(internalClassName);
        return true;
    }

    /**
     * 获取指定类的所有注入规范。
     * 
     * @param className 类名（内部格式，斜杠分隔，如 "com/example/Foo"）
     * @return 注入规范列表的快照副本（不可为 null）
     */
    public List<InjectionSpec> getSpecsForClass(String className) {
        List<InjectionSpec> specs = specsByClass.get(className);
        return specs != null ? new ArrayList<>(specs) : Collections.<InjectionSpec>emptyList();
    }

    /**
     * 获取所有注入规范。
     * 
     * @return 所有注入规范的快照副本
     */
    public Collection<InjectionSpec> getAllSpecs() {
        return new ArrayList<>(specsById.values());
    }

    /**
     * 检查指定类是否有注入点。
     * 
     * @param className 类名（内部格式，斜杠分隔）
     * @return true 表示该类至少有一个注入点
     */
    public boolean hasInjectionsForClass(String className) {
        List<InjectionSpec> specs = specsByClass.get(className);
        return specs != null && !specs.isEmpty();
    }

    /**
     * 清空所有注入规范。
     * 
     * <p>同时清理本地索引和 Agent 端注册表，并对所有受影响的类触发重新转换。
     */
    public void clear() {
        Set<String> classNames = new HashSet<>(specsByClass.keySet());
        specsById.clear();
        specsByClass.clear();
        clearAgentRegistry();
        for (String className : classNames) {
            triggerRetransform(className);
        }
    }

    // ==================== Agent 同步 ====================

    /**
     * 延迟初始化 Agent 端注册表的连接。
     * 
     * <p>通过反射加载 AgentRegistry 类并获取其 INSTANCE、register、unregister 方法。
     * 使用双重检查锁确保线程安全的单次初始化。
     * 
     * <p>初始化失败（如 Agent 未加载）不会抛出异常，仅记录警告日志，
     * 后续同步操作将静默跳过。
     */
    private void initAgentRegistry() {
        if (agentInitialized) return;
        synchronized (InjectionRegistry.class) {
            if (agentInitialized) return;
            try {
                Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(AGENT_REGISTRY);
                Field instanceField = clazz.getField("INSTANCE");
                agentRegistry = instanceField.get(null);
                registerMethod = clazz.getMethod("register", String.class, String.class, String.class, String.class, String.class);
                unregisterMethod = clazz.getMethod("unregister", String.class, String.class);
                // LOGGER.info("已连接到 Agent Registry");
            } catch (Exception e) {
                LOGGER.warning("无法连接到 Agent Registry: " + e.getMessage());
            }
            agentInitialized = true;
        }
    }

    /**
     * 将注入规范同步到 Agent 端注册表。
     * 
     * @param className 类名（内部格式，斜杠分隔）
     * @param spec 注入规范
     */
    private void syncToAgent(String className, InjectionSpec spec) {
        initAgentRegistry();
        if (agentRegistry == null || registerMethod == null) return;
        try {
            registerMethod.invoke(agentRegistry, className, spec.getId(), spec.getMethodName(), spec.getMethodDescriptor(), spec.getType().name());
        } catch (Exception e) {
            LOGGER.warning("同步到 Agent 失败: " + e.getMessage());
        }
    }

    /**
     * 从 Agent 端注册表移除指定注入。
     * 
     * @param className 类名（内部格式，斜杠分隔）
     * @param id 注入规范 ID
     */
    private void unsyncFromAgent(String className, String id) {
        initAgentRegistry();
        if (agentRegistry == null || unregisterMethod == null) return;
        try {
            unregisterMethod.invoke(agentRegistry, className, id);
        } catch (Exception e) {
            LOGGER.warning("从 Agent 移除失败: " + e.getMessage());
        }
    }

    /**
     * 清空 Agent 端注册表的所有注入。
     */
    private void clearAgentRegistry() {
        initAgentRegistry();
        if (agentRegistry == null) return;
        try {
            Method clearMethod = agentRegistry.getClass().getMethod("clear");
            clearMethod.invoke(agentRegistry);
        } catch (Exception ignored) {
        }
    }

    // ==================== Retransform ====================

    /**
     * 触发类的重新转换，使注入生效。
     * 如果类尚未加载，则在首次加载时自动应用注入。
     *
     * @param className 类名（内部格式，斜杠分隔）
     */
    private void triggerRetransform(String className) {
        if (!InstrumentationBridge.isAvailable()) {
            LOGGER.warning("Instrumentation 不可用，无法触发 retransform");
            return;
        }
        String dotClassName = className.replace('/', '.');
        // LOGGER.info("正在查找类: " + dotClassName);
        Class<?> targetClass = findLoadedClass(dotClassName);
        if (targetClass == null) {
            // LOGGER.info("目标类尚未加载，将在首次加载时注入: " + dotClassName);
            return;
        }
        try {
            InstrumentationBridge.retransformClasses(targetClass);
            // LOGGER.info("已触发类重新转换: " + dotClassName);
        } catch (Exception e) {
            LOGGER.warning("重新转换类失败: " + e.getMessage());
        }
    }

    /**
     * 查找已加载的类。
     * 先尝试通过 Class.forName 加载，失败则遍历所有已加载类查找。
     *
     * @param dotClassName 类名（点分隔格式）
     * @return 已加载的类，如果未找到则返回 null
     */
    private Class<?> findLoadedClass(String dotClassName) {
        try {
            // 使用 initialize=false 避免触发类初始化
            return Class.forName(dotClassName, false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException ignored) {
        }
        // 遍历所有已加载的类进行查找
        Instrumentation inst = InstrumentationBridge.get();
        if (inst == null) return null;
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (clazz.getName().equals(dotClassName)) {
                return clazz;
            }
        }
        return null;
    }
}
