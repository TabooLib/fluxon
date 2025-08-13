package org.tabooproject.fluxon.tool;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Function;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 函数导出工具 - 简化版
 */
public class FunctionDumper {

    private final FluxonRuntime runtime;
    private final StringBuilder output;

    public FunctionDumper() {
        this.runtime = FluxonRuntime.getInstance();
        this.output = new StringBuilder();
    }

    public void dumpToFile(String filePath) throws IOException {
        output.append("# Fluxon 函数列表\n\n");
        
        collectSystemFunctions();
        collectExtensionFunctions();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(output.toString());
        }
        
        System.out.println("Functions dumped to: " + filePath);
    }

    private void collectSystemFunctions() {
        Map<String, Function> systemFunctions = runtime.getSystemFunctions();
        TreeMap<String, Function> sortedFunctions = new TreeMap<>(systemFunctions);
        
        output.append("## 系统函数\n");
        
        List<String> funcList = new ArrayList<>();
        for (Map.Entry<String, Function> entry : sortedFunctions.entrySet()) {
            String name = entry.getKey();
            Function function = entry.getValue();
            List<Integer> paramCounts = function.getParameterCounts();
            
            if (paramCounts.size() == 1 && paramCounts.get(0) == 0) {
                funcList.add(name);
            } else if (paramCounts.size() == 1) {
                funcList.add(name + "(" + paramCounts.get(0) + ")");
            } else {
                funcList.add(name + "(" + paramCounts.stream().map(String::valueOf).collect(Collectors.joining("|")) + ")");
            }
        }
        
        output.append(String.join(", ", funcList)).append("\n\n");
    }

    private void collectExtensionFunctions() {
        Map<Class<?>, Map<String, Function>> extensionFunctions = runtime.getExtensionFunctions();
        
        output.append("## 扩展函数\n");
        
        TreeMap<String, Map.Entry<Class<?>, Map<String, Function>>> sortedByClassName = new TreeMap<>();
        for (Map.Entry<Class<?>, Map<String, Function>> entry : extensionFunctions.entrySet()) {
            sortedByClassName.put(entry.getKey().getSimpleName(), entry);
        }
        
        for (Map.Entry<String, Map.Entry<Class<?>, Map<String, Function>>> classEntry : sortedByClassName.entrySet()) {
            String className = classEntry.getKey();
            Map<String, Function> methods = classEntry.getValue().getValue();
            
            List<String> methodList = new ArrayList<>();
            for (Map.Entry<String, Function> methodEntry : methods.entrySet()) {
                String methodName = methodEntry.getKey();
                List<Integer> params = methodEntry.getValue().getParameterCounts();
                if (params.size() == 1) {
                    methodList.add(methodName + "(" + params.get(0) + ")");
                } else {
                    methodList.add(methodName + "(" + params.stream().map(String::valueOf).collect(Collectors.joining("|")) + ")");
                }
            }
            
            output.append("- **").append(className).append("**: ")
                  .append(String.join(", ", methodList)).append("\n");
        }
        
        output.append("\n");
    }


    /**
     * 主程序入口
     */
    public static void main(String[] args) {
        String outputFile = "fluxon_functions.md";
        
        if (args.length > 0) {
            outputFile = args[0];
        }
        
        FunctionDumper dumper = new FunctionDumper();
        try {
            dumper.dumpToFile(outputFile);
            System.out.println("Successfully exported functions to: " + outputFile);
        } catch (IOException e) {
            System.err.println("Failed to export functions: " + e.getMessage());
            e.printStackTrace();
        }
    }
}