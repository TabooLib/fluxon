package org.tabooproject.fluxon.runtime;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.parser.SymbolFunction;
import org.tabooproject.fluxon.runtime.function.*;
import org.tabooproject.fluxon.runtime.function.domain.DomainExtension;
import org.tabooproject.fluxon.runtime.function.extension.*;
import org.tabooproject.fluxon.runtime.function.extension.reflect.ExtensionClass;
import org.tabooproject.fluxon.runtime.function.extension.reflect.ExtensionConstructor;
import org.tabooproject.fluxon.runtime.function.extension.reflect.ExtensionField;
import org.tabooproject.fluxon.runtime.function.extension.reflect.ExtensionMethod;
import org.tabooproject.fluxon.runtime.java.ExportRegistry;
import org.tabooproject.fluxon.runtime.reflection.SecurityPolicy;
import org.tabooproject.fluxon.runtime.library.LibraryLoader;
import org.tabooproject.fluxon.runtime.library.LibraryLoader.LibraryLoadResult;
import org.tabooproject.fluxon.runtime.sharing.SharedFunctionAdapter;
import org.tabooproject.fluxon.runtime.sharing.SharedFunctionEntry;
import org.tabooproject.fluxon.runtime.sharing.SharedFunctionRegistry;
import org.tabooproject.fluxon.util.KV;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 原生函数和符号注册中心
 * 用于统一管理解析阶段和执行阶段的内置函数和符号
 */
public class FluxonRuntime {

    // 类型
    public static final Type TYPE = new Type(FluxonRuntime.class);

    // 单例实例
    private static final FluxonRuntime INSTANCE = new FluxonRuntime();

    // 系统函数（按名称分组的重载集合）
    private final Map<String, OverloadSet> systemFunctions = new LinkedHashMap<>();
    // 系统变量
    private final Map<String, Object> systemVariables = new HashMap<>();
    // 扩展函数（支持重载）
    private final Map<String, Map<Class<?>, OverloadSet>> extensionFunctions = new LinkedHashMap<>();
    // 全局类型短名，供所有脚本共享默认 Java 类型别名。
    private final Map<String, Class<?>> typeAliases = new LinkedHashMap<>();

    // 缓存的系统函数数组（避免每次创建环境时都转换）
    private volatile Function[] cachedSystemFunctions;
    // 缓存的系统扩展函数数组（避免每次创建环境时都转换）
    private volatile KV<Class<?>, Function>[][] cachedSystemExtensionFunctions;
    // 缓存的扩展函数派发表数组（用于优化扩展函数解析）
    private volatile ExtensionDispatchTable[] cachedDispatchTables;
    // 脏标记：当注册新函数时标记为 true，下次创建环境时会重新构建缓存
    private volatile boolean dirty = false;
    // 注册锁定标记：锁定后禁止注册新函数，用于定位并发注册问题
    private volatile boolean registrationLocked = false;

    // Export 注册中心
    private final ExportRegistry exportRegistry = new ExportRegistry(this);

    // 主线程执行器
    private Executor primaryThreadExecutor = Executors.newSingleThreadExecutor();

    // 安全策略
    private SecurityPolicy securityPolicy = SecurityPolicy.ALLOW_ALL;

    // 共享身份标识
    private String sharingIdentity;

    /**
     * 获取单例实例
     */
    public static FluxonRuntime getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化系统函数
     */
    private FluxonRuntime() {
        // 全局对象
        // 用于应对在上下文环境中使用同名的全局函数
        registerFunction("g", FunctionSignature.returns(Type.OBJECT).noParams(), context -> context.setReturnRef(GlobalObject.INSTANCE));
        // reflect
        ExtensionClass.init(this);
        ExtensionConstructor.init(this);
        ExtensionField.init(this);
        ExtensionMethod.init(this);
        // Extension
        ExtensionCollection.init(this);
        ExtensionFile.init(this);
        ExtensionIterable.init(this);
        ExtensionList.init(this);
        ExtensionMap.init(this);
        ExtensionMapEntry.init(this);
        ExtensionObject.init(this);
        ExtensionPath.init(this);
        ExtensionString.init(this);
        ExtensionThrowable.init(this);
        // Function
        FunctionCrypto.init(this);
        FunctionEnvironment.init(this);
        FunctionFile.init(this);
        FunctionMath.init(this);
        FunctionSystem.init(this);
        FunctionTime.init(this);
        FunctionType.init(this);
        // Domain
        DomainExtension.init();
    }

    private void checkRegistrationLock() {
        if (registrationLocked) {
            throw new IllegalStateException("Function registration is locked. Unlock before registering new functions.");
        }
    }

    /**
     * 构建缓存的函数和扩展函数数组
     * 这样每次创建环境时就不需要重新转换了
     */
    @SuppressWarnings("unchecked")
    private void bake() {
        // 构建系统函数数组（展平所有重载）
        List<Function> allFunctions = new ArrayList<>();
        for (OverloadSet set : systemFunctions.values()) {
            allFunctions.addAll(set.getOverloads());
        }
        cachedSystemFunctions = allFunctions.toArray(new Function[0]);
        // 构建系统扩展函数数组和派发表
        List<KV<Class<?>, Function>[]> systemExtensionFunctionsList = new ArrayList<>();
        List<ExtensionDispatchTable> dispatchTablesList = new ArrayList<>();
        for (Map.Entry<String, Map<Class<?>, OverloadSet>> entry : extensionFunctions.entrySet()) {
            Map<Class<?>, OverloadSet> classOverloadSetMap = entry.getValue();
            int size = classOverloadSetMap.size();
            // 构建候选数组（保持注册顺序）
            Class<?>[] candidateClasses = new Class<?>[size];
            OverloadSet[] candidateOverloadSets = new OverloadSet[size];
            List<KV<Class<?>, Function>> candidatesList = new ArrayList<>();
            int i = 0;
            for (Map.Entry<Class<?>, OverloadSet> entry2 : classOverloadSetMap.entrySet()) {
                candidateClasses[i] = entry2.getKey();
                candidateOverloadSets[i] = entry2.getValue();
                // 为了兼容旧代码，将重载集合中的所有函数都添加到 candidatesList
                for (Function f : entry2.getValue().getOverloads()) {
                    candidatesList.add(new KV<>(entry2.getKey(), f));
                }
                i++;
            }
            systemExtensionFunctionsList.add(candidatesList.toArray(new KV[0]));
            // 构建派发表
            dispatchTablesList.add(new ExtensionDispatchTable(classOverloadSetMap, candidateClasses, candidateOverloadSets));
        }
        cachedSystemExtensionFunctions = systemExtensionFunctionsList.toArray(new KV[0][]);
        cachedDispatchTables = dispatchTablesList.toArray(new ExtensionDispatchTable[0]);
        // 重置脏标记
        dirty = false;
    }

    /**
     * 确保缓存数组与当前注册状态一致。
     * 在读取缓存数组前调用，避免 dirty 状态下读取到旧索引表。
     */
    private void ensureBaked() {
        if (dirty || cachedSystemFunctions == null || cachedSystemExtensionFunctions == null || cachedDispatchTables == null) {
            synchronized (this) {
                if (dirty || cachedSystemFunctions == null || cachedSystemExtensionFunctions == null || cachedDispatchTables == null) {
                    bake();
                }
            }
        }
    }

    /**
     * 初始化解释器环境
     */
    public Environment newEnvironment() {
        ensureBaked();
        return new Environment(systemFunctions, systemVariables);
    }

    /**
     * 注册变量
     *
     * @param name  变量名
     * @param value 变量值
     */
    public void registerVariable(String name, Object value) {
        systemVariables.put(name, value);
    }

    /**
     * 锁定函数注册，锁定后任何注册操作会抛出异常（含完整堆栈）
     */
    public void lockRegistration() {
        registrationLocked = true;
    }

    /**
     * 解锁函数注册
     */
    public void unlockRegistration() {
        registrationLocked = false;
    }

    /**
     * 注册已有的函数实例
     *
     * @param function 函数实例
     */
    public synchronized void registerFunction(@NotNull Function function) {
        checkRegistrationLock();
        systemFunctions.computeIfAbsent(function.getName(), OverloadSet::new).add(function);
        dirty = true;
    }

    /**
     * 注册系统函数
     */
    public synchronized void registerFunction(String name, FunctionSignature signature, NativeFunction.NativeCallable<?> implementation) {
        checkRegistrationLock();
        systemFunctions.computeIfAbsent(name, OverloadSet::new).add(new NativeFunction<>(name, signature, implementation));
        dirty = true;
    }

    /**
     * 注册系统函数（带直接绑定，编译器可内联为 INVOKESTATIC）
     */
    public synchronized void registerFunction(String name, FunctionSignature signature, NativeFunction.NativeCallable<?> implementation, DirectBinding directBinding) {
        checkRegistrationLock();
        systemFunctions.computeIfAbsent(name, OverloadSet::new).add(new NativeFunction<>(name, signature, implementation, directBinding));
        dirty = true;
    }

    /**
     * 注册系统函数（带命名空间）
     */
    public synchronized void registerFunction(String namespace, String name, FunctionSignature signature, NativeFunction.NativeCallable<?> implementation) {
        checkRegistrationLock();
        systemFunctions.computeIfAbsent(name, OverloadSet::new).add(new NativeFunction<>(namespace, name, signature, implementation));
        dirty = true;
    }

    /**
     * 注册异步系统函数
     */
    public synchronized void registerAsyncFunction(String name, FunctionSignature signature, NativeFunction.NativeCallable<?> implementation) {
        checkRegistrationLock();
        systemFunctions.computeIfAbsent(name, OverloadSet::new).add(new NativeFunction<>(null, name, signature, implementation, true, false));
        dirty = true;
    }

    /**
     * 注册主线程同步系统函数
     */
    public synchronized void registerPrimarySyncFunction(String name, FunctionSignature signature, NativeFunction.NativeCallable<?> implementation) {
        checkRegistrationLock();
        systemFunctions.computeIfAbsent(name, OverloadSet::new).add(new NativeFunction<>(null, name, signature, implementation, false, true));
        dirty = true;
    }

    /**
     * 注册扩展函数
     */
    public <Target> ExtensionBuilder<Target> registerExtension(Class<Target> extensionClass) {
        return new ExtensionBuilder<>(this, extensionClass, null);
    }

    /**
     * 注册扩展函数
     */
    public <Target> ExtensionBuilder<Target> registerExtension(Class<Target> extensionClass, String namespace) {
        return new ExtensionBuilder<>(this, extensionClass, namespace);
    }

    /**
     * 注册扩展函数，直接使用已有 Function 实例
     */
    public synchronized <Target> void registerExtensionFunction(Class<Target> extensionClass, Function function) {
        extensionFunctions.computeIfAbsent(function.getName(), k -> new LinkedHashMap<>())
                .computeIfAbsent(extensionClass, c -> new OverloadSet(function.getName()))
                .add(function);
        dirty = true;
    }

    /**
     * 注册扩展函数
     */
    public synchronized <Target> void registerExtensionFunction(
            Class<Target> extensionClass,
            String namespace,
            String name,
            FunctionSignature signature,
            NativeFunction.NativeCallable<Target> implementation,
            boolean isAsync,
            boolean isPrimarySync) {
        extensionFunctions.computeIfAbsent(name, k -> new LinkedHashMap<>())
                .computeIfAbsent(extensionClass, c -> new OverloadSet(name))
                .add(new NativeFunction<>(namespace, name, signature, implementation, isAsync, isPrimarySync));
        dirty = true;
    }

    /**
     * 卸载已注册的系统函数（仅当映射中仍指向同一实例时）
     *
     * @param function 待卸载的函数实例
     * @return 是否成功卸载
     */
    public synchronized boolean unregisterFunction(@NotNull Function function) {
        OverloadSet set = systemFunctions.get(function.getName());
        if (set == null) {
            return false;
        }
        boolean removed = set.remove(function);
        if (removed) {
            if (set.isEmpty()) {
                systemFunctions.remove(function.getName());
            }
            dirty = true;
        }
        return removed;
    }

    /**
     * 卸载已注册的扩展函数（仅当映射中仍指向同一实例时）
     *
     * @param extensionClass 扩展目标类型
     * @param name           函数名称
     * @param function       待卸载的函数实例
     * @return 是否成功卸载
     */
    public synchronized boolean unregisterExtensionFunction(@NotNull Class<?> extensionClass, @NotNull String name, @NotNull Function function) {
        Map<Class<?>, OverloadSet> classFunctions = extensionFunctions.get(name);
        if (classFunctions == null) {
            return false;
        }
        OverloadSet overloadSet = classFunctions.get(extensionClass);
        if (overloadSet == null) {
            return false;
        }
        boolean removed = overloadSet.remove(function);
        if (removed) {
            if (overloadSet.isEmpty()) {
                classFunctions.remove(extensionClass);
            }
            if (classFunctions.isEmpty()) {
                extensionFunctions.remove(name);
            }
            dirty = true;
        }
        return removed;
    }

    /**
     * 获取所有函数重载集合
     */
    public Map<String, OverloadSet> getSystemFunctions() {
        return systemFunctions;
    }

    /**
     * 获取所有变量信息
     */
    public Map<String, Object> getSystemVariables() {
        return systemVariables;
    }

    /**
     * 获取所有扩展函数信息
     */
    public Map<String, Map<Class<?>, OverloadSet>> getExtensionFunctions() {
        return extensionFunctions;
    }

    /**
     * 获取缓存的系统函数数组
     */
    public Function[] getCachedSystemFunctions() {
        ensureBaked();
        return cachedSystemFunctions;
    }

    /**
     * 获取缓存的系统扩展函数数组
     */
    @SuppressWarnings("unchecked")
    public KV<Class<?>, Function>[][] getCachedSystemExtensionFunctions() {
        ensureBaked();
        return cachedSystemExtensionFunctions;
    }

    /**
     * 获取缓存的扩展函数派发表
     */
    public ExtensionDispatchTable[] getCachedDispatchTables() {
        ensureBaked();
        return cachedDispatchTables;
    }

    /**
     * 获取 Export 注册中心
     */
    public ExportRegistry getExportRegistry() {
        return exportRegistry;
    }

    /**
     * 注册全局 Java 类型短名。
     * 单脚本 CompilationContext 中的同名别名优先级更高，可用于覆盖全局默认。
     *
     * @param alias 类型短名
     * @param type  目标 Java 类型
     * @return this
     */
    public synchronized FluxonRuntime registerTypeAlias(@NotNull String alias, @NotNull Class<?> type) {
        checkRegistrationLock();
        typeAliases.put(alias, type);
        return this;
    }

    /**
     * 移除全局 Java 类型短名。
     *
     * @param alias 类型短名
     * @return 被移除的类型，未注册时返回 null
     */
    public synchronized Class<?> unregisterTypeAlias(@NotNull String alias) {
        checkRegistrationLock();
        return typeAliases.remove(alias);
    }

    /**
     * 获取全局 Java 类型短名。
     *
     * @param alias 类型短名
     * @return 注册的 Java 类型，未注册时返回 null
     */
    public synchronized Class<?> getTypeAlias(@NotNull String alias) {
        return typeAliases.get(alias);
    }

    /**
     * 获取全局 Java 类型短名快照。
     */
    public synchronized Map<String, Class<?>> getTypeAliases() {
        return new LinkedHashMap<>(typeAliases);
    }

    /**
     * 获取主线程执行器
     */
    public Executor getPrimaryThreadExecutor() {
        return primaryThreadExecutor;
    }

    /**
     * 设置主线程执行器
     */
    public void setPrimaryThreadExecutor(Executor primaryThreadExecutor) {
        this.primaryThreadExecutor = primaryThreadExecutor;
    }

    /**
     * 获取安全策略
     */
    public SecurityPolicy getSecurityPolicy() {
        return securityPolicy;
    }

    /**
     * 设置安全策略
     * 传入 null 时回退到 ALLOW_ALL
     */
    public void setSecurityPolicy(SecurityPolicy policy) {
        this.securityPolicy = policy != null ? policy : SecurityPolicy.ALLOW_ALL;
    }

    /**
     * 编译并加载库文件，将其中的 @api 函数注册到运行时
     *
     * @param path 库文件路径
     * @return 加载结果
     */
    public LibraryLoadResult loadLibrary(Path path) {
        return new LibraryLoader(this).load(path);
    }

    // region 函数共享

    /**
     * 设置共享身份标识
     */
    public void setSharingIdentity(String identity) {
        this.sharingIdentity = identity;
    }

    /**
     * 获取共享身份标识
     */
    public String getSharingIdentity() {
        return sharingIdentity;
    }

    /**
     * 导出函数（直接传 MethodHandle）
     */
    public void exportFunction(String name, MethodHandle handle) {
        requireSharingIdentity();
        SharedFunctionRegistry.register(sharingIdentity, name, handle);
    }

    /**
     * 导出扩展函数（直接传 MethodHandle）
     */
    public void exportExtensionFunction(String name, MethodHandle handle, Class<?> extensionTarget) {
        requireSharingIdentity();
        SharedFunctionRegistry.registerExtension(sharingIdentity, name, handle, extensionTarget);
    }

    /**
     * 导出已注册的普通函数
     * 内部通过 MethodHandle 包装 Function.call()
     */
    public void exportRegisteredFunction(String name) {
        requireSharingIdentity();
        OverloadSet set = systemFunctions.get(name);
        if (set == null || set.isEmpty()) {
            throw new IllegalArgumentException("Function not found: " + name);
        }
        Function function = set.first();
        try {
            MethodHandle mh = MethodHandles.lookup().findStatic(
                    FluxonRuntime.class,
                    "invokeSharedFunction",
                    MethodType.methodType(Object.class, Function.class, Object[].class));
            mh = mh.bindTo(function).asVarargsCollector(Object[].class);
            SharedFunctionRegistry.register(sharingIdentity, name, mh);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to create MethodHandle for shared function: " + name, e);
        }
    }

    /**
     * 导出已注册的扩展函数
     */
    public void exportRegisteredExtensionFunction(String name, Class<?> extensionTarget) {
        requireSharingIdentity();
        Map<Class<?>, OverloadSet> classFunctions = extensionFunctions.get(name);
        if (classFunctions == null) {
            throw new IllegalArgumentException("Extension function not found: " + name);
        }
        OverloadSet set = classFunctions.get(extensionTarget);
        if (set == null || set.isEmpty()) {
            throw new IllegalArgumentException("Extension function not found: " + name + " for " + extensionTarget.getName());
        }
        Function function = set.first();
        try {
            MethodHandle mh = MethodHandles.lookup().findStatic(
                    FluxonRuntime.class,
                    "invokeSharedExtensionFunction",
                    MethodType.methodType(Object.class, Function.class, Object.class, Object[].class));
            mh = mh.bindTo(function).asVarargsCollector(Object[].class);
            SharedFunctionRegistry.registerExtension(sharingIdentity, name, mh, extensionTarget);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to create MethodHandle for shared extension function: " + name, e);
        }
    }

    /**
     * 从共享注册表导入函数
     */
    public boolean importSharedFunction(String owner, String name) {
        Object[] entry = SharedFunctionRegistry.find(owner, name);
        if (entry == null) return false;
        importEntry(entry);
        return true;
    }

    /**
     * 导入指定 owner 的所有共享函数
     */
    public int importAllSharedFunctions(String owner) {
        int count = 0;
        String prefix = owner + ":";
        for (Map.Entry<String, Object[]> e : SharedFunctionRegistry.getGlobalRegistry().entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                importEntry(e.getValue());
                count++;
            }
        }
        return count;
    }

    /**
     * 导入所有 owner 的所有共享函数
     */
    public int importAllSharedFunctions() {
        int count = 0;
        for (Map.Entry<String, Object[]> e : SharedFunctionRegistry.getGlobalRegistry().entrySet()) {
            importEntry(e.getValue());
            count++;
        }
        return count;
    }

    /**
     * 将 entry 适配并注册到本地运行时
     */
    private void importEntry(Object[] entry) {
        NativeFunction<?> adapted = SharedFunctionAdapter.adapt(entry);
        if (SharedFunctionEntry.isExtension(entry)) {
            Class<?> targetClass = SharedFunctionEntry.extensionTarget(entry);
            registerExtensionFunction(targetClass, adapted);
        } else {
            registerFunction(adapted);
        }
    }

    /**
     * 卸载所有已导出的共享函数
     */
    public void unexportAll() {
        if (sharingIdentity != null) {
            SharedFunctionRegistry.unregisterAll(sharingIdentity);
        }
    }

    private void requireSharingIdentity() {
        if (sharingIdentity == null) {
            throw new IllegalStateException("Sharing identity not set. Call setSharingIdentity() first.");
        }
    }

    /**
     * 内部桥接：通过 FunctionContext 调用普通函数并返回结果
     */
    static Object invokeSharedFunction(Function function, Object... args) {
        FunctionContextPool pool = FunctionContextPool.local();
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try (FunctionContext<?> ctx = pool.borrow(function, null, args, env)) {
            function.call(ctx);
            if (ctx.hasPrimitiveReturn()) {
                return ctx.boxReturnPrimitive();
            }
            return ctx.getReturnRef();
        }
    }

    /**
     * 内部桥接：通过 FunctionContext 调用扩展函数并返回结果
     */
    static Object invokeSharedExtensionFunction(Function function, Object target, Object... args) {
        FunctionContextPool pool = FunctionContextPool.local();
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        try (FunctionContext<?> ctx = pool.borrow(function, target, args, env)) {
            function.call(ctx);
            if (ctx.hasPrimitiveReturn()) {
                return ctx.boxReturnPrimitive();
            }
            return ctx.getReturnRef();
        }
    }

    // endregion
}
