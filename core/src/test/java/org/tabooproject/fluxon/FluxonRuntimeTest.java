package org.tabooproject.fluxon;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Arrays;

public class FluxonRuntimeTest {

    public static void registerTestFunctions() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        // checkGrade 函数
        runtime.registerFunction("checkGrade", 1, args -> {
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
        runtime.registerFunction("player", Arrays.asList(1, 3), args -> {
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
        runtime.registerFunction("fetch", 1, args -> {
            if (args.length > 0) {
                String url = String.valueOf(args[0]);
                return "Fetching data from " + url;
            }
            throw new RuntimeException("fetch function requires a URL parameter");
        });
    }
}
