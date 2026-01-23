package org.tabooproject.fluxon.type;

import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Arrays;
import java.util.Objects;

public class TestRuntime {

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
            Object arg0 = context.getRef(0);
            if (arg0 instanceof Number) {
                int score = ((Number) arg0).intValue();
                if (score >= 90) { context.setReturnRef("Excellent"); return; }
                if (score >= 80) { context.setReturnRef("Good"); return; }
                if (score >= 70) { context.setReturnRef("Fair"); return; }
                if (score >= 60) { context.setReturnRef("Pass"); return; }
                context.setReturnRef("Fail");
                return;
            }
            throw new RuntimeException("checkGrade function requires a numeric argument");
        });
        // player 函数 - 支持多种参数数量
        runtime.registerFunction("player", Arrays.asList(1, 3), (context) -> {
            int argc = context.getArgumentCount();
            if (argc >= 1) {
                String playerName = String.valueOf(context.getRef(0));
                if (argc >= 3) {
                    context.setReturnRef("Player " + playerName + " HP: " + context.getRef(1) + ", Level: " + context.getRef(2));
                    return;
                }
                context.setReturnRef("Player " + playerName);
                return;
            }
            throw new RuntimeException("player function requires at least one argument");
        });
        // fetch 函数
        runtime.registerFunction("fetch", 1, (context) -> {
            Object arg0 = context.getRef(0);
            if (arg0 != null) {
                String url = String.valueOf(arg0);
                context.setReturnRef("Fetching data from " + url);
                return;
            }
            throw new RuntimeException("fetch function requires a URL parameter");
        });
        runtime.registerFunction("location", 3, (context) -> {
            Object a0 = context.getRef(0);
            Object a1 = context.getRef(1);
            Object a2 = context.getRef(2);
            context.setReturnRef(new TestLocation(
                    a0 instanceof Number ? ((Number) a0).doubleValue() : 0,
                    a1 instanceof Number ? ((Number) a1).doubleValue() : 0,
                    a2 instanceof Number ? ((Number) a2).doubleValue() : 0
            ));
        });
        runtime.registerExtensionFunction(TestAudience.class, "location", 0, (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getLocation()));
        runtime.registerExtensionFunction(TestAudience.class, "x", 0, (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getLocation().getY()));
        runtime.registerExtensionFunction(TestAudience.class, "y", 0, (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getLocation().getZ()));
        runtime.registerExtensionFunction(TestAudience.class, "z", 0, (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getLocation().getZ()));
        runtime.registerExtensionFunction(TestLocation.class, "x", 0, (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getY()));
        runtime.registerExtensionFunction(TestLocation.class, "y", 0, (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getZ()));
        runtime.registerExtensionFunction(TestLocation.class, "z", 0, (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getZ()));
        runtime.registerExtensionFunction(TestLocation.class, "yaw", 0, (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getYaw()));
        runtime.registerExtensionFunction(TestLocation.class, "pitch", 0, (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getPitch()));
    }
}
