package org.tabooproject.fluxon;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;
import static org.tabooproject.fluxon.runtime.FunctionSignature.returnsObject;

/**
 * 函数重载解析测试
 *
 * @author sky
 */
public class OverloadResolutionTest {

    // 模拟 World 类型
    public static class MockWorld {
        private final String name;

        public MockWorld(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "World:" + name;
        }
    }

    // 模拟有 world() 方法的对象
    public static class MockCenter {
        private final MockWorld world;

        public MockCenter(MockWorld world) {
            this.world = world;
        }

        public MockWorld world() {
            return world;
        }
    }

    // 模拟 Vector 类型
    public static class MockVector {
        public final double x, y, z;

        public MockVector(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public MockVector multiply(MockVector other) {
            return new MockVector(x * other.x, y * other.y, z * other.z);
        }

        public MockVector multiply(double d) {
            return new MockVector(x * d, y * d, z * d);
        }

        @Override
        public String toString() {
            return "Vector(" + x + "," + y + "," + z + ")";
        }
    }

    @BeforeAll
    static void setup() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        // 注册测试函数：三个重载
        // overload(String) -> "string:" + arg
        runtime.registerFunction("overload", returns(Type.STRING).params(Type.STRING), ctx -> {
            ctx.setReturnRef("string:" + ctx.getString(0));
        });
        // overload(int) -> "int:" + arg
        runtime.registerFunction("overload", returns(Type.STRING).params(Type.I), ctx -> {
            ctx.setReturnRef("int:" + ctx.getInt(0));
        });
        // overload(int, int, int) -> "triple:" + sum
        runtime.registerFunction("overload", returns(Type.STRING).params(Type.I, Type.I, Type.I), ctx -> {
            ctx.setReturnRef("triple:" + (ctx.getInt(0) + ctx.getInt(1) + ctx.getInt(2)));
        });
        // player(String) -> "name:" + arg
        runtime.registerFunction("player", returnsObject().params(Type.STRING), ctx -> {
            ctx.setReturnRef("name:" + ctx.getString(0));
        });
        // player(UUID) -> "uuid:" + arg
        runtime.registerFunction("player", returnsObject().params(Type.fromClass(UUID.class)), ctx -> {
            ctx.setReturnRef("uuid:" + ctx.getRef(0));
        });
        // location(x, y, z) -> "xyz:" + coords
        runtime.registerFunction("location", returns(Type.STRING).params(Type.D, Type.D, Type.D), ctx -> {
            ctx.setReturnRef("xyz:" + ctx.getDouble(0) + "," + ctx.getDouble(1) + "," + ctx.getDouble(2));
        });
        // location(world, x, y, z) -> "world-xyz:" + world + coords
        runtime.registerFunction("location", returns(Type.STRING).params(Type.fromClass(MockWorld.class), Type.D, Type.D, Type.D), ctx -> {
            ctx.setReturnRef("world-xyz:" + ctx.getRef(0) + "," + ctx.getDouble(1) + "," + ctx.getDouble(2) + "," + ctx.getDouble(3));
        });
        // 扩展函数：MockVector::multiply(MockVector)
        runtime.registerExtensionFunction(MockVector.class, null, "multiply",
                returns(Type.fromClass(MockVector.class)).params(Type.fromClass(MockVector.class)), ctx -> {
            MockVector target = (MockVector) ctx.getTarget();
            MockVector other = (MockVector) ctx.getRef(0);
            ctx.setReturnRef(target.multiply(other));
        }, false, false);
        // 扩展函数：MockVector::multiply(D)
        runtime.registerExtensionFunction(MockVector.class, null, "multiply",
                returns(Type.fromClass(MockVector.class)).params(Type.D), ctx -> {
            MockVector target = (MockVector) ctx.getTarget();
            double d = ctx.getDouble(0);
            ctx.setReturnRef(target.multiply(d));
        }, false, false);
    }

    @Test
    void testOverloadWithStringLiteral() {
        // 调用 overload("#dc9dfc") 应该匹配 overload(String)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("overload(\"#dc9dfc\")");
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("string:#dc9dfc", result.getInterpretResult());
        assertEquals("string:#dc9dfc", result.getCompileResult());
    }

    @Test
    void testOverloadWithIntLiteral() {
        // 调用 overload(123) 应该匹配 overload(int)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("overload(123)");
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("int:123", result.getInterpretResult());
        assertEquals("int:123", result.getCompileResult());
    }

    @Test
    void testOverloadWithTripleInt() {
        // 调用 overload(1, 2, 3) 应该匹配 overload(int, int, int)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("overload(1, 2, 3)");
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("triple:6", result.getInterpretResult());
        assertEquals("triple:6", result.getCompileResult());
    }

    @Test
    void testPlayerWithStringLiteral() {
        // 调用 player("test") 应该匹配 player(String)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("player(\"test\")");
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("name:test", result.getInterpretResult());
        assertEquals("name:test", result.getCompileResult());
    }

    @Test
    void testPlayerWithDynamicStringVariable() {
        // 变量类型未知时，应该在运行时正确解析到 player(String)
        String script = "key = \"test\"\nplayer(&key)";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("name:test", result.getInterpretResult());
        assertEquals("name:test", result.getCompileResult());
    }

    @Test
    void testPlayerWithDynamicUUIDVariable() {
        // 变量类型未知时，应该在运行时正确解析到 player(UUID)
        UUID testUUID = UUID.randomUUID();
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("player(&key)", ctx -> {}, env -> {
            env.setRootVariable("key", testUUID);
        });
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("uuid:" + testUUID, result.getInterpretResult());
        assertEquals("uuid:" + testUUID, result.getCompileResult());
    }

    @Test
    void testOverloadWithRoundResults() {
        // round() 返回 long，但 overload(int, int, int) 期望 int
        // 应该自动收窄转换
        String script = "currentR = 255.5\ncurrentG = 128.3\ncurrentB = 64.7\noverload(round(&currentR), round(&currentG), round(&currentB))";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(script);
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        // round(255.5)=256, round(128.3)=128, round(64.7)=65, sum=449
        assertEquals("triple:449", result.getInterpretResult());
        assertEquals("triple:449", result.getCompileResult());
    }

    @Test
    void testRoundReturnType() {
        // 测试 round() 的返回类型
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("round(1.5)");
        System.out.println("Interpret type: " + result.getInterpretResult().getClass());
        System.out.println("Compile type: " + result.getCompileResult().getClass());
        assertEquals(2L, result.getInterpretResult());
        assertEquals(2L, result.getCompileResult());
    }

    @Test
    void testOverloadWithLongLiterals() {
        // 直接测试 long 参数
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("overload(1L, 2L, 3L)");
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("triple:6", result.getInterpretResult());
        assertEquals("triple:6", result.getCompileResult());
    }

    @Test
    void testLocationWithDynamicWorldType() {
        // 测试 location(&center::world(), &x, &y, &z) 的场景
        // 当 &center 的类型未知时，world() 的返回类型也未知
        // 应该正确解析到 location(World, D, D, D) 而不是 location(D, D, D)
        MockWorld world = new MockWorld("test_world");
        MockCenter center = new MockCenter(world);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "location(&world, &x, &y, &z)",
                ctx -> {},
                env -> {
                    env.setRootVariable("world", world);
                    env.setRootVariable("x", 1.0);
                    env.setRootVariable("y", 2.0);
                    env.setRootVariable("z", 3.0);
                }
        );
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("world-xyz:World:test_world,1.0,2.0,3.0", result.getInterpretResult());
        assertEquals("world-xyz:World:test_world,1.0,2.0,3.0", result.getCompileResult());
    }

    @Test
    void testLocationWithoutWorld() {
        // 测试不带 world 的 location(x, y, z)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "location(&x, &y, &z)",
                ctx -> {},
                env -> {
                    env.setRootVariable("x", 1.0);
                    env.setRootVariable("y", 2.0);
                    env.setRootVariable("z", 3.0);
                }
        );
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("xyz:1.0,2.0,3.0", result.getInterpretResult());
        assertEquals("xyz:1.0,2.0,3.0", result.getCompileResult());
    }

    @Test
    void testLocationWithMethodCallOnDynamicObject() {
        // 更接近实际场景：location(&center::world(), &x, &y, &z)
        // &center 类型未知，world() 返回类型也未知，编译时全是 OBJECT
        // 注：world() 作为扩展函数需要单独注册，这里简化测试
        MockWorld world = new MockWorld("test_world");
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "location(&world, &x, &y, &z)",
                ctx -> {},
                env -> {
                    env.setRootVariable("world", world);
                    env.setRootVariable("x", 1.0);
                    env.setRootVariable("y", 2.0);
                    env.setRootVariable("z", 3.0);
                }
        );
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        assertEquals("world-xyz:World:test_world,1.0,2.0,3.0", result.getInterpretResult());
        assertEquals("world-xyz:World:test_world,1.0,2.0,3.0", result.getCompileResult());
    }

    @Test
    void testExtensionFunctionOverloadWithDynamicArg() {
        // 复现问题：&vec::multiply(&a)
        // &a 是 double 表达式结果，编译时类型是 OBJECT
        // 应该匹配 multiply(D) 而不是 multiply(Vector)
        MockVector vec = new MockVector(2.0, 3.0, 4.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 0.5\n&vec::multiply(&a)",
                ctx -> {},
                env -> {
                    env.setRootVariable("vec", vec);
                }
        );
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        // 2.0*0.5=1.0, 3.0*0.5=1.5, 4.0*0.5=2.0
        assertEquals("Vector(1.0,1.5,2.0)", result.getInterpretResult().toString());
        assertEquals("Vector(1.0,1.5,2.0)", result.getCompileResult().toString());
    }
}
