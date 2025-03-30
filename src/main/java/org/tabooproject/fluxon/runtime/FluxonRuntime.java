package org.tabooproject.fluxon.runtime;

import org.tabooproject.fluxon.interpreter.Function;
import org.tabooproject.fluxon.parser.SymbolInfo;
import org.tabooproject.fluxon.parser.SymbolType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 原生函数和符号注册中心
 * 用于统一管理解析阶段和执行阶段的内置函数和符号
 */
public class FluxonRuntime {
    
    // 单例实例
    private static final FluxonRuntime INSTANCE = new FluxonRuntime();
    
    // 注册的符号信息（解析阶段使用）
    private final Map<String, SymbolInfo> symbolInfoMap = new HashMap<>();
    
    // 注册的函数实现（执行阶段使用）
    private final Map<String, Function> functionMap = new HashMap<>();
    
    /**
     * 获取单例实例
     * 
     * @return 注册中心实例
     */
    public static FluxonRuntime getInstance() {
        return INSTANCE;
    }
    
    /**
     * 私有构造函数，初始化内置函数
     */
    private FluxonRuntime() {
        registerDefaultFunctions();
    }
    
    /**
     * 注册内置函数
     */
    private void registerDefaultFunctions() {
        // print 函数
        registerFunction("print", 1, args -> {
            if (args.length > 0) {
                System.out.println(args[0]);
            } else {
                System.out.println();
            }
            return null;
        });
        
        // checkGrade 函数
        registerFunction("checkGrade", 1, args -> {
            if (args.length > 0 && args[0] instanceof Number) {
                int score = ((Number) args[0]).intValue();
                if (score >= 90) return "Excellent";
                if (score >= 80) return "Good"; 
                if (score >= 70) return "Fair";
                if (score >= 60) return "Pass";
                return "Fail";
            }
            throw new RuntimeException("checkGrade function requires a numeric argument");
        });
        
        // player 函数 - 支持多种参数数量
        registerFunction("player", Arrays.asList(1, 3), args -> {
            if (args.length >= 1) {
                String playerName = String.valueOf(args[0]);
                if (args.length >= 3) {
                    return "Player " + playerName + " HP: " + args[1] + ", Level: " + args[2];
                }
                return "Player " + playerName;
            }
            throw new RuntimeException("player function requires at least one argument");
        });
        
        // fetch 函数
        registerFunction("fetch", 1, args -> {
            if (args.length > 0) {
                String url = String.valueOf(args[0]);
                return "Fetching data from " + url;
            }
            throw new RuntimeException("fetch function requires a URL parameter");
        });
    }
    
    /**
     * 注册原生函数（使用指定的符号信息）
     * 
     * @param name 函数名
     * @param symbolInfo 符号信息（用于解析阶段）
     * @param implementation 函数实现（用于执行阶段）
     */
    public void registerNativeFunction(String name, SymbolInfo symbolInfo, NativeFunction.NativeCallable implementation) {
        symbolInfoMap.put(name, symbolInfo);
        functionMap.put(name, new NativeFunction(implementation));
    }
    
    /**
     * 注册函数 - 简化版（接受单个参数数量）
     * 
     * @param name 函数名
     * @param paramCount 参数数量
     * @param implementation 函数实现
     */
    public void registerFunction(String name, int paramCount, NativeFunction.NativeCallable implementation) {
        registerNativeFunction(
            name, 
            new SymbolInfo(SymbolType.FUNCTION, name, paramCount), 
            implementation
        );
    }
    
    /**
     * 注册函数 - 简化版（接受多个可能的参数数量）
     * 
     * @param name 函数名
     * @param paramCounts 可能的参数数量列表
     * @param implementation 函数实现
     */
    public void registerFunction(String name, List<Integer> paramCounts, NativeFunction.NativeCallable implementation) {
        registerNativeFunction(
            name, 
            new SymbolInfo(SymbolType.FUNCTION, name, paramCounts), 
            implementation
        );
    }
    
    /**
     * 获取所有符号信息（用于初始化解析器）
     * 
     * @return 符号信息映射表
     */
    public Map<String, SymbolInfo> getSymbolInfoMap() {
        return new HashMap<>(symbolInfoMap);
    }
    
    /**
     * 初始化解释器环境
     * 
     * @param environment 要初始化的环境
     */
    public void initializeEnvironment(org.tabooproject.fluxon.interpreter.Environment environment) {
        for (Map.Entry<String, Function> entry : functionMap.entrySet()) {
            environment.define(entry.getKey(), entry.getValue());
        }
    }
} 