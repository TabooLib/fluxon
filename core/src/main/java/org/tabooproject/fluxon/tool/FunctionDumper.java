package org.tabooproject.fluxon.tool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionSignature;
import org.tabooproject.fluxon.runtime.OverloadSet;
import org.tabooproject.fluxon.runtime.Type;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 将系统与扩展函数导出为 JSON 目录
 */
public class FunctionDumper {

    private final FluxonRuntime runtime;

    public FunctionDumper() {
        this.runtime = FluxonRuntime.getInstance();
    }

    public void dumpToFile(String filePath) throws IOException {
        String json = buildCatalogJson();
        Path target = Paths.get(filePath);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(target, json.getBytes(StandardCharsets.UTF_8));
        System.out.println("Functions dumped to: " + target.toAbsolutePath());
    }

    private String buildCatalogJson() {
        Catalog catalog = new Catalog();
        catalog.generatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        catalog.system = collectSystemFunctions();
        catalog.extensions = collectExtensionFunctions();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        return gson.toJson(catalog);
    }

    private List<CatalogFunction> collectSystemFunctions() {
        TreeMap<String, OverloadSet> system = new TreeMap<>(runtime.getSystemFunctions());
        List<CatalogFunction> result = new ArrayList<>();
        for (Map.Entry<String, OverloadSet> entry : system.entrySet()) {
            for (Function function : entry.getValue().getOverloads()) {
                result.add(toCatalogFunction(entry.getKey(), function));
            }
        }
        return result;
    }

    private Map<String, List<CatalogFunction>> collectExtensionFunctions() {
        Map<String, Map<Class<?>, Function>> rawExtensions = runtime.getExtensionFunctions();
        TreeMap<String, Map<String, Function>> grouped = new TreeMap<>();
        for (Map.Entry<String, Map<Class<?>, Function>> entry : rawExtensions.entrySet()) {
            String functionName = entry.getKey();
            for (Map.Entry<Class<?>, Function> classEntry : entry.getValue().entrySet()) {
                String owner = classEntry.getKey().getName();
                grouped.computeIfAbsent(owner, key -> new TreeMap<>())
                    .put(functionName, classEntry.getValue());
            }
        }
        Map<String, List<CatalogFunction>> result = new LinkedHashMap<>();
        grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                List<Map.Entry<String, Function>> sorted = new ArrayList<>(entry.getValue().entrySet());
                sorted.sort(Comparator.comparing(Map.Entry::getKey));
                List<CatalogFunction> functions = new ArrayList<>(sorted.size());
                for (Map.Entry<String, Function> fn : sorted) {
                    functions.add(toCatalogFunction(fn.getKey(), fn.getValue()));
                }
                result.put(entry.getKey(), functions);
            });
        return result;
    }

    private CatalogFunction toCatalogFunction(String name, Function function) {
        CatalogFunction catalogFunction = new CatalogFunction();
        catalogFunction.name = name;
        catalogFunction.namespace = emptyToNull(function.getNamespace());
        FunctionSignature sig = function.getSignature();
        if (sig != null) {
            catalogFunction.returnType = sig.getReturnType().toString();
            Type[] paramTypes = sig.getParameterTypes();
            catalogFunction.paramTypes = new String[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                catalogFunction.paramTypes[i] = paramTypes[i].toString();
            }
        }
        catalogFunction.async = function.isAsync();
        catalogFunction.primarySync = function.isPrimarySync();
        return catalogFunction;
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    public static void main(String[] args) {
        String outputFile = "build/fluxon-functions.json";
        if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            outputFile = args[0];
        }
        FunctionDumper dumper = new FunctionDumper();
        try {
            dumper.dumpToFile(outputFile);
        } catch (IOException e) {
            System.err.println("Failed to export functions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class Catalog {
        String generatedAt;
        List<CatalogFunction> system;
        Map<String, List<CatalogFunction>> extensions;
    }

    private static class CatalogFunction {
        String name;
        String namespace;
        String returnType;
        String[] paramTypes;
        boolean async;
        boolean primarySync;
    }
}