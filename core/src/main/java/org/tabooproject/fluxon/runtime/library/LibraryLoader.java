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
import org.tabooproject.fluxon.runtime.EnvironmentPool;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 负责编译并加载 Fluxon 库文件，
 * 将其中标记了 @api 的函数注册到运行时。
 */
public class LibraryLoader {

    private final FluxonRuntime runtime;
    private final ClassLoader parentClassLoader;

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
            String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            String className = deriveClassName(path);
            // 编译库文件
            CompileResult compileResult;
            try (EnvironmentPool.Lease lease = runtime.borrowEnvironment()) {
                compileResult = Fluxon.compile(source, className, lease.get(), parentClassLoader);
            }
            // 使用新的类加载器隔离库
            FluxonClassLoader loader = new FluxonClassLoader(parentClassLoader);
            Class<?> scriptClass = compileResult.defineClass(loader);
            // 触发 <clinit>，确保函数单例被创建
            RuntimeScriptBase scriptInstance = (RuntimeScriptBase) scriptClass.getDeclaredConstructor().newInstance();
            // 从静态字段中提取 @api 函数并注册
            List<Function> exportedFunctions = registerApiFunctions(compileResult, scriptClass);
            return new LibraryLoadResult(path, scriptInstance, exportedFunctions);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load library: " + path, e);
        }
    }

    /**
     * 从编译结果中提取 @api 函数并注册到运行时。
     *
     * @param compileResult 编译结果
     * @param scriptClass   脚本类
     * @return 导出的函数列表
     */
    private List<Function> registerApiFunctions(CompileResult compileResult, Class<?> scriptClass) throws Exception {
        List<Function> exported = new ArrayList<>();
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
                    targetClass = Class.forName(bindTarget, true, parentClassLoader);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Failed to load bind target class: " + bindTarget, e);
                }
                runtime.registerExtensionFunction((Class<?>) targetClass, function);
            } else {
                runtime.registerFunction(function);
            }
            exported.add(function);
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

    /**
     * 描述一次库加载的结果。
     */
    public static class LibraryLoadResult {
        private final Path sourcePath;
        private final RuntimeScriptBase scriptInstance;
        private final List<Function> exportedFunctions;

        public LibraryLoadResult(Path sourcePath, RuntimeScriptBase scriptInstance, List<Function> exportedFunctions) {
            this.sourcePath = sourcePath;
            this.scriptInstance = scriptInstance;
            this.exportedFunctions = exportedFunctions;
        }

        public Path getSourcePath() {
            return sourcePath;
        }

        public RuntimeScriptBase getScriptInstance() {
            return scriptInstance;
        }

        public List<Function> getExportedFunctions() {
            return exportedFunctions;
        }
    }
}
