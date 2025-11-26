package org.tabooproject.fluxon.runtime.library;

import org.jetbrains.annotations.NotNull;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.CompileResult;
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 负责编译并加载 Fluxon 库文件，
 * 将其中标记了 @api 的函数注册到运行时。
 */
public class LibraryLoader {

    private final FluxonRuntime runtime;
    private final ClassLoader parentClassLoader;
    private final List<LibraryLoadResult> managedResults = Collections.synchronizedList(new ArrayList<>());

    public LibraryLoader(FluxonRuntime runtime) {
        this(runtime, FluxonRuntime.class.getClassLoader());
    }

    public LibraryLoader(FluxonRuntime runtime, ClassLoader parentClassLoader) {
        this.runtime = runtime;
        this.parentClassLoader = parentClassLoader;
    }

    /**
     * 编译并加载库文件，将 @api 函数注册到运行时。
     *
     * @param path 库文件路径
     * @return 加载结果
     */
    public LibraryLoadResult load(Path path) {
        try {
            FluxonClassLoader classLoader = new FluxonClassLoader(parentClassLoader);
            String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            String className = deriveClassName(path);
            // 编译库文件
            Environment env = runtime.newEnvironment();
            CompileResult compileResult = Fluxon.compile(source, className, env, parentClassLoader);
            Class<?> scriptClass = compileResult.defineClass(classLoader);
            // 触发 <clinit>，确保函数单例被创建
            RuntimeScriptBase scriptInstance = (RuntimeScriptBase) scriptClass.getDeclaredConstructor().newInstance();
            // 从静态字段中提取 @api 函数并注册
            List<ExportedFunction> exportedFunctions = registerApiFunctions(compileResult, scriptClass, classLoader);
            LibraryLoadResult result = new LibraryLoadResult(runtime, path, scriptInstance, exportedFunctions, this::untrackResult);
            trackResult(result);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load library: " + path, e);
        }
    }

    /**
     * 重新加载库：先卸载旧结果，再加载新版本。
     *
     * @param path     库文件路径
     * @param previous 之前的加载结果（可为空）
     * @return 新的加载结果
     */
    public LibraryLoadResult reload(Path path, LibraryLoadResult previous) {
        if (previous != null) {
            previous.unload();
        }
        return load(path);
    }

    /**
     * 卸载并清理当前记录的所有加载结果。
     */
    public void unloadManagedResults() {
        List<LibraryLoadResult> snapshot;
        synchronized (managedResults) {
            snapshot = new ArrayList<>(managedResults);
        }
        for (LibraryLoadResult result : snapshot) {
            result.unload();
        }
    }

    /**
     * 返回当前记录的加载结果快照。
     */
    public List<LibraryLoadResult> getManagedResults() {
        synchronized (managedResults) {
            return Collections.unmodifiableList(new ArrayList<>(managedResults));
        }
    }

    /**
     * 从编译结果中提取 @api 函数并注册到运行时。
     *
     * @param compileResult 编译结果
     * @param scriptClass   脚本类
     * @return 导出的函数列表
     */
    private List<ExportedFunction> registerApiFunctions(CompileResult compileResult, Class<?> scriptClass, ClassLoader bindClassLoader) throws Exception {
        List<ExportedFunction> exported = new ArrayList<>();
        for (Definition definition : compileResult.getGenerator().getDefinitions()) {
            if (!(definition instanceof FunctionDefinition)) {
                continue;
            }
            FunctionDefinition functionDefinition = (FunctionDefinition) definition;
            Annotation apiAnnotation = findApiAnnotation(functionDefinition);
            if (apiAnnotation == null) {
                continue;
            }
            Function function = getFunction(scriptClass, functionDefinition);
            String bindTarget = getBindTarget(apiAnnotation);
            if (bindTarget != null) {
                Class<?> targetClass;
                try {
                    targetClass = Class.forName(bindTarget, false, bindClassLoader);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Failed to load bind target class: " + bindTarget, e);
                }
                runtime.registerExtensionFunction((Class<?>) targetClass, function);
                exported.add(new ExportedFunction(function, targetClass));
            } else {
                runtime.registerFunction(function);
                exported.add(new ExportedFunction(function, null));
            }
        }
        return Collections.unmodifiableList(exported);
    }

    /**
     * 从脚本类中获取函数实例。
     *
     * @param scriptClass        脚本类
     * @param functionDefinition 函数定义
     * @return 函数实例
     */
    private @NotNull Function getFunction(Class<?> scriptClass, FunctionDefinition functionDefinition) throws NoSuchFieldException, IllegalAccessException {
        if (!functionDefinition.isRegisterToRoot()) {
            throw new IllegalStateException("Function " + functionDefinition.getName() + " is marked with @api but not registered to root");
        }
        Field field = scriptClass.getField(functionDefinition.getName());
        Object value = field.get(null);
        if (!(value instanceof Function)) {
            throw new IllegalStateException("Field " + functionDefinition.getName() + " is not a Function");
        }
        return (Function) value;
    }

    /**
     * 从函数定义中提取 @api 注解。
     *
     * @param definition 函数定义
     * @return @api 注解实例，若不存在则返回 null
     */
    private Annotation findApiAnnotation(FunctionDefinition definition) {
        for (Annotation annotation : definition.getAnnotations()) {
            if ("api".equalsIgnoreCase(annotation.getName())) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * 从 @api 注解中提取绑定目标。
     *
     * @param apiAnnotation @api 注解实例
     * @return 绑定目标字符串，若不存在则返回 null
     */
    private String getBindTarget(Annotation apiAnnotation) {
        Object bind = apiAnnotation.getAttributes().get("bind");
        if (bind instanceof String) {
            String target = ((String) bind).trim();
            return target.isEmpty() ? null : target;
        }
        return null;
    }

    /**
     * 从库文件路径中提取类名。
     * 例如: "src/test/fs/library.fs" -> "library"
     */
    private String deriveClassName(Path path) {
        String name = path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        if (idx > 0) {
            name = name.substring(0, idx);
        }
        name = name.replaceAll("[^A-Za-z0-9_]", "_");
        if (name.isEmpty()) {
            return "fluxon_library";
        }
        // 保持简洁的内部类名，避免非法字符
        return name;
    }

    private void trackResult(LibraryLoadResult result) {
        managedResults.add(result);
    }

    private void untrackResult(LibraryLoadResult result) {
        managedResults.remove(result);
    }

    /**
     * 描述一次库加载的结果。
     */
    public static class LibraryLoadResult implements AutoCloseable {

        private final FluxonRuntime runtime;
        private final Path sourcePath;
        private final RuntimeScriptBase scriptInstance;
        private final List<ExportedFunction> exportedFunctions;
        private final List<Function> exportedFunctionView;
        private final Consumer<LibraryLoadResult> onUnload;
        private boolean unloaded;

        public LibraryLoadResult(FluxonRuntime runtime, Path sourcePath, RuntimeScriptBase scriptInstance, List<ExportedFunction> exportedFunctions, Consumer<LibraryLoadResult> onUnload) {
            this.runtime = runtime;
            this.sourcePath = sourcePath;
            this.scriptInstance = scriptInstance;
            this.exportedFunctions = exportedFunctions;
            this.onUnload = onUnload;
            List<Function> view = new ArrayList<>(exportedFunctions.size());
            for (ExportedFunction exportedFunction : exportedFunctions) {
                view.add(exportedFunction.function);
            }
            this.exportedFunctionView = Collections.unmodifiableList(view);
        }

        public Path getSourcePath() {
            return sourcePath;
        }

        public RuntimeScriptBase getScriptInstance() {
            return scriptInstance;
        }

        public List<Function> getExportedFunctions() {
            return exportedFunctionView;
        }

        /**
         * 卸载本次加载注册的函数，释放运行时引用，便于 GC 回收相关类加载器。
         * 可重复调用，后续调用将被忽略。
         */
        public void unload() {
            if (unloaded) {
                return;
            }
            for (ExportedFunction exportedFunction : exportedFunctions) {
                if (exportedFunction.bindTarget != null) {
                    runtime.unregisterExtensionFunction(exportedFunction.bindTarget, exportedFunction.function.getName(), exportedFunction.function);
                } else {
                    runtime.unregisterFunction(exportedFunction.function);
                }
            }
            unloaded = true;
            if (onUnload != null) {
                onUnload.accept(this);
            }
        }

        @Override
        public void close() {
            unload();
        }
    }

    /**
     * 描述一个导出的函数，包含函数实例和绑定目标（若有）。
     */
    public static final class ExportedFunction {
        private final Function function;
        private final Class<?> bindTarget;

        private ExportedFunction(Function function, Class<?> bindTarget) {
            this.function = function;
            this.bindTarget = bindTarget;
        }

        public Function getFunction() {
            return function;
        }

        public Class<?> getBindTarget() {
            return bindTarget;
        }
    }
}
