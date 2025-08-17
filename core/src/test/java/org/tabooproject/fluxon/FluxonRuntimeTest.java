package org.tabooproject.fluxon;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Arrays;
import java.util.Objects;

public class FluxonRuntimeTest {

    public static class TestAudience {

        private final TestLocation location;

        public TestAudience(TestLocation location) {
            this.location = location;
        }

        public TestLocation getLocation() {
            return location;
        }

        @Override
        public String toString() {
            return "TestAudience{" +
                    "location=" + location +
                    '}';
        }
    }

    public static class TestLocation {

        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        public TestLocation(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = 0;
            this.pitch = 0;
        }

        public TestLocation(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
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

        public float getYaw() {
            return yaw;
        }

        public float getPitch() {
            return pitch;
        }

        @Override
        public String toString() {
            return "TestLocation{" +
                    "x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    ", yaw=" + yaw +
                    ", pitch=" + pitch +
                    '}';
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
        runtime.registerFunction("location", 3, (context) -> {
            Object[] args = context.getArguments();
            return new TestLocation(
                    args[0] instanceof Number ? ((Number) args[0]).doubleValue() : 0,
                    args[1] instanceof Number ? ((Number) args[1]).doubleValue() : 0,
                    args[2] instanceof Number ? ((Number) args[2]).doubleValue() : 0
            );
        });
        runtime.registerExtensionFunction(TestAudience.class, "location", 0, (context) -> Objects.requireNonNull(context.getTarget()).getLocation());
        runtime.registerExtensionFunction(TestAudience.class, "x", 0, (context) -> Objects.requireNonNull(context.getTarget()).getLocation().getY());
        runtime.registerExtensionFunction(TestAudience.class, "y", 0, (context) -> Objects.requireNonNull(context.getTarget()).getLocation().getZ());
        runtime.registerExtensionFunction(TestAudience.class, "z", 0, (context) -> Objects.requireNonNull(context.getTarget()).getLocation().getZ());
        runtime.registerExtensionFunction(TestLocation.class, "x", 0, (context) -> Objects.requireNonNull(context.getTarget()).getY());
        runtime.registerExtensionFunction(TestLocation.class, "y", 0, (context) -> Objects.requireNonNull(context.getTarget()).getZ());
        runtime.registerExtensionFunction(TestLocation.class, "z", 0, (context) -> Objects.requireNonNull(context.getTarget()).getZ());
        runtime.registerExtensionFunction(TestLocation.class, "yaw", 0, (context) -> Objects.requireNonNull(context.getTarget()).getYaw());
        runtime.registerExtensionFunction(TestLocation.class, "pitch", 0, (context) -> Objects.requireNonNull(context.getTarget()).getPitch());
    }
}
