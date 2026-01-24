package org.tabooproject.fluxon.type;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.util.Objects;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

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
        runtime.registerFunction("checkGrade", returns(Type.OBJECT).params(Type.OBJECT), (context) -> {
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
        runtime.registerFunction("player", returns(Type.OBJECT).params(Type.OBJECT), (context) -> {
            String playerName = String.valueOf(context.getRef(0));
            context.setReturnRef("Player " + playerName);
        });
        runtime.registerFunction("player", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT, Type.OBJECT), (context) -> {
            String playerName = String.valueOf(context.getRef(0));
            context.setReturnRef("Player " + playerName + " HP: " + context.getRef(1) + ", Level: " + context.getRef(2));
        });
        // fetch 函数
        runtime.registerFunction("fetch", returns(Type.OBJECT).params(Type.OBJECT), (context) -> {
            Object arg0 = context.getRef(0);
            if (arg0 != null) {
                String url = String.valueOf(arg0);
                context.setReturnRef("Fetching data from " + url);
                return;
            }
            throw new RuntimeException("fetch function requires a URL parameter");
        });
        runtime.registerFunction("location", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT, Type.OBJECT), (context) -> {
            Object a0 = context.getRef(0);
            Object a1 = context.getRef(1);
            Object a2 = context.getRef(2);
            context.setReturnRef(new TestLocation(
                    a0 instanceof Number ? ((Number) a0).doubleValue() : 0,
                    a1 instanceof Number ? ((Number) a1).doubleValue() : 0,
                    a2 instanceof Number ? ((Number) a2).doubleValue() : 0
            ));
        });
        runtime.registerExtensionFunction(TestAudience.class, null, "location", returns(Type.OBJECT).noParams(), (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getLocation()), false, false);
        runtime.registerExtensionFunction(TestAudience.class, null, "x", returns(Type.D).noParams(), (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getLocation().getY()), false, false);
        runtime.registerExtensionFunction(TestAudience.class, null, "y", returns(Type.D).noParams(), (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getLocation().getZ()), false, false);
        runtime.registerExtensionFunction(TestAudience.class, null, "z", returns(Type.D).noParams(), (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getLocation().getZ()), false, false);
        runtime.registerExtensionFunction(TestLocation.class, null, "x", returns(Type.D).noParams(), (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getY()), false, false);
        runtime.registerExtensionFunction(TestLocation.class, null, "y", returns(Type.D).noParams(), (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getZ()), false, false);
        runtime.registerExtensionFunction(TestLocation.class, null, "z", returns(Type.D).noParams(), (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getZ()), false, false);
        runtime.registerExtensionFunction(TestLocation.class, null, "yaw", returns(Type.F).noParams(), (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getYaw()), false, false);
        runtime.registerExtensionFunction(TestLocation.class, null, "pitch", returns(Type.F).noParams(), (context) -> context.setReturnRef(Objects.requireNonNull(context.getTarget()).getPitch()), false, false);
    }
}
