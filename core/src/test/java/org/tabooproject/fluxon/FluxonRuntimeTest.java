package org.tabooproject.fluxon;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.FunctionContext;

import java.util.Arrays;
import java.util.Objects;
import java.util.Vector;

public class FluxonRuntimeTest {

    public static class TestVector {

        private final double x;
        private final double y;
        private final double z;

        public TestVector(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }
    }

    public static void registerTestFunctions() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        // checkGrade 函数
        runtime.registerFunction("checkGrade", 1, (context) -> {
            Object[] args = context.getArguments();
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
        runtime.registerFunction("player", Arrays.asList(1, 3), (context) -> {
            Object[] args = context.getArguments();
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
        runtime.registerFunction("fetch", 1, (context) -> {
            Object[] args = context.getArguments();
            if (args.length > 0) {
                String url = String.valueOf(args[0]);
                return "Fetching data from " + url;
            }
            throw new RuntimeException("fetch function requires a URL parameter");
        });
        runtime.registerFunction("vec3", 3, (context) -> {
            Object[] args = context.getArguments();
            return new TestVector(
                    args[0] instanceof Number ? ((Number) args[0]).doubleValue() : 0,
                    args[1] instanceof Number ? ((Number) args[1]).doubleValue() : 0,
                    args[2] instanceof Number ? ((Number) args[2]).doubleValue() : 0
            );
        });
        runtime.registerExtensionFunction(TestVector.class, "x", 0, (context) -> Objects.requireNonNull(context.getTarget()).getX());
        runtime.registerExtensionFunction(TestVector.class, "y", 0, (context) -> Objects.requireNonNull(context.getTarget()).getY());
        runtime.registerExtensionFunction(TestVector.class, "z", 0, (context) -> Objects.requireNonNull(context.getTarget()).getZ());
    }
}
