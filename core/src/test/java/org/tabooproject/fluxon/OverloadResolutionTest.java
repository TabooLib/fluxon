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
            MockVector target = ctx.getTarget();
            MockVector other = (MockVector) ctx.getRef(0);
            ctx.setReturnRef(target.multiply(other));
        }, false, false);
        // 扩展函数：MockVector::multiply(D)
        runtime.registerExtensionFunction(MockVector.class, null, "multiply",
                returns(Type.fromClass(MockVector.class)).params(Type.D), ctx -> {
            MockVector target = ctx.getTarget();
            double d = ctx.getDouble(0);
            ctx.setReturnRef(target.multiply(d));
        }, false, false);
        // 扩展函数：MockVector::add(MockVector) — 链式调用测试
        runtime.registerExtensionFunction(MockVector.class, null, "add",
                returns(Type.fromClass(MockVector.class)).params(Type.fromClass(MockVector.class)), ctx -> {
            MockVector target = ctx.getTarget();
            MockVector other = (MockVector) ctx.getRef(0);
            ctx.setReturnRef(new MockVector(target.x + other.x, target.y + other.y, target.z + other.z));
        }, false, false);
        // 扩展函数：MockVector::scale(I) — int 参数
        runtime.registerExtensionFunction(MockVector.class, null, "scale",
                returns(Type.fromClass(MockVector.class)).params(Type.I), ctx -> {
            MockVector target = ctx.getTarget();
            int i = ctx.getInt(0);
            ctx.setReturnRef(new MockVector(target.x * i, target.y * i, target.z * i));
        }, false, false);
        // 扩展函数：MockVector::scale(D) — double 参数
        runtime.registerExtensionFunction(MockVector.class, null, "scale",
                returns(Type.fromClass(MockVector.class)).params(Type.D), ctx -> {
            MockVector target = ctx.getTarget();
            double d = ctx.getDouble(0);
            ctx.setReturnRef(new MockVector(target.x * d, target.y * d, target.z * d));
        }, false, false);
        // 扩展函数：MockVector::x()
        runtime.registerExtensionFunction(MockVector.class, null, "x",
                returns(Type.D).noParams(), ctx -> {
            MockVector target = ctx.getTarget();
            ctx.setReturnRef(target.x);
        }, false, false);
        // 扩展函数：MockVector::y()
        runtime.registerExtensionFunction(MockVector.class, null, "y",
                returns(Type.D).noParams(), ctx -> {
            MockVector target = ctx.getTarget();
            ctx.setReturnRef(target.y);
        }, false, false);
        // 扩展函数：MockVector::z()
        runtime.registerExtensionFunction(MockVector.class, null, "z",
                returns(Type.D).noParams(), ctx -> {
            MockVector target = ctx.getTarget();
            ctx.setReturnRef(target.z);
        }, false, false);
        // compute(String, D) -> "sd:" + ...
        runtime.registerFunction("compute", returns(Type.STRING).params(Type.STRING, Type.D), ctx -> {
            ctx.setReturnRef("sd:" + ctx.getString(0) + "," + ctx.getDouble(1));
        });
        // compute(String, I) -> "si:" + ...
        runtime.registerFunction("compute", returns(Type.STRING).params(Type.STRING, Type.I), ctx -> {
            ctx.setReturnRef("si:" + ctx.getString(0) + "," + ctx.getInt(1));
        });
        // compute(String, String) -> "ss:" + ...
        runtime.registerFunction("compute", returns(Type.STRING).params(Type.STRING, Type.STRING), ctx -> {
            ctx.setReturnRef("ss:" + ctx.getString(0) + "," + ctx.getString(1));
        });
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

    @Test
    void testRandomWithDynamicDoubleArgs() {
        // 测试 random(-&offsetJitter, &offsetJitter) 场景
        // offsetJitter 通过 &?var ?: default 语法定义
        // 问题：编译器可能选择 random(I, I) 而不是 random(D, D)
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "offsetJitter = &?offsetJitter ?: 0.5\nrandom(-&offsetJitter, &offsetJitter)",
                ctx -> {},
                env -> {}
        );
        System.out.println("Interpret: " + result.getInterpretResult() + " (" + result.getInterpretResult().getClass().getSimpleName() + ")");
        System.out.println("Compile: " + result.getCompileResult() + " (" + result.getCompileResult().getClass().getSimpleName() + ")");
        // 结果应该是 double 类型，在 [-0.5, 0.5) 范围内
        assertInstanceOf(Double.class, result.getInterpretResult(), "Interpret should return Double, got: " + result.getInterpretResult().getClass());
        assertInstanceOf(Double.class, result.getCompileResult(), "Compile should return Double, got: " + result.getCompileResult().getClass());
        double interpretVal = ((Number) result.getInterpretResult()).doubleValue();
        double compileVal = ((Number) result.getCompileResult()).doubleValue();
        assertTrue(interpretVal >= -0.5 && interpretVal < 0.5, "Interpret result out of range: " + interpretVal);
        assertTrue(compileVal >= -0.5 && compileVal < 0.5, "Compile result out of range: " + compileVal);
    }

    @Test
    void testRandomWithDynamicIntArgs() {
        // 用整数测试，如果选了 random(I, I)，-1 到 1 的范围内只有 -1 和 0
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "random(-&offsetJitter, &offsetJitter)",
                ctx -> {},
                env -> {
                    env.setRootVariable("offsetJitter", 5);
                }
        );
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        // 整数结果应该在 [-5, 5) 范围内
        int interpretVal = ((Number) result.getInterpretResult()).intValue();
        int compileVal = ((Number) result.getCompileResult()).intValue();
        assertTrue(interpretVal >= -5 && interpretVal < 5, "Interpret result out of range: " + interpretVal);
        assertTrue(compileVal >= -5 && compileVal < 5, "Compile result out of range: " + compileVal);
    }

    @Test
    void testSystemFunctionWithExtensionFunctionSameName() {
        // 复现问题：random 既是系统函数也是扩展函数 (Collection::random)
        // 在 :: 链式调用环境中，当参数类型未知时，不应该错误地使用 DeferredExtensionHandler
        // 关键：在扩展函数链内调用 random
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "val = 0.5\n&vec::multiply(random(-&val, &val))",
                ctx -> {},
                env -> {
                    env.setRootVariable("vec", new MockVector(1.0, 1.0, 1.0));
                }
        );
        System.out.println("Interpret: " + result.getInterpretResult());
        System.out.println("Compile: " + result.getCompileResult());
        // 应该正常执行，不报错
        assertNotNull(result.getCompileResult());
    }

    @Test
    void testExtensionChainWithLiterals() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::multiply(2.0)::multiply(3.0)",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // (1*2*3, 2*2*3, 3*2*3) = (6.0, 12.0, 18.0)
        assertEquals("Vector(6.0,12.0,18.0)", result.getInterpretResult().toString());
        assertEquals("Vector(6.0,12.0,18.0)", result.getCompileResult().toString());
    }

    @Test
    void testExtensionChainWithDynamicArgs() {
        // 链式调用，每步延迟解析（注入变量）
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::multiply(&factor)::multiply(&factor)",
                ctx -> {},
                env -> {
                    env.setRootVariable("vec", vec);
                    env.setRootVariable("factor", 2.0);
                }
        );
        // (1*2*2, 2*2*2, 3*2*2) = (4.0, 8.0, 12.0)
        assertEquals("Vector(4.0,8.0,12.0)", result.getInterpretResult().toString());
        assertEquals("Vector(4.0,8.0,12.0)", result.getCompileResult().toString());
    }

    @Test
    void testExtensionChainMixedFunctions() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        MockVector offset = new MockVector(10.0, 20.0, 30.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::add(&offset)::multiply(2.0)",
                ctx -> {},
                env -> {
                    env.setRootVariable("vec", vec);
                    env.setRootVariable("offset", offset);
                }
        );
        // (1+10, 2+20, 3+30) * 2 = (22.0, 44.0, 66.0)
        assertEquals("Vector(22.0,44.0,66.0)", result.getInterpretResult().toString());
        assertEquals("Vector(22.0,44.0,66.0)", result.getCompileResult().toString());
    }

    @Test
    void testExtensionChainThreeSteps() {
        MockVector vec = new MockVector(1.0, 1.0, 1.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 2.0\n&vec::multiply(&a)::multiply(&a)::multiply(&a)",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // 1 * 2 * 2 * 2 = 8.0
        assertEquals("Vector(8.0,8.0,8.0)", result.getInterpretResult().toString());
        assertEquals("Vector(8.0,8.0,8.0)", result.getCompileResult().toString());
    }

    @Test
    void testExtensionChainWithVectorArg() {
        MockVector vec = new MockVector(2.0, 3.0, 4.0);
        MockVector other = new MockVector(5.0, 6.0, 7.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::multiply(&other)",
                ctx -> {},
                env -> {
                    env.setRootVariable("vec", vec);
                    env.setRootVariable("other", other);
                }
        );
        // (2*5, 3*6, 4*7) = (10.0, 18.0, 28.0)
        assertEquals("Vector(10.0,18.0,28.0)", result.getInterpretResult().toString());
        assertEquals("Vector(10.0,18.0,28.0)", result.getCompileResult().toString());
    }

    @Test
    void testNestedExtensionCallVectorResult() {
        MockVector vec1 = new MockVector(2.0, 3.0, 4.0);
        MockVector vec2 = new MockVector(1.0, 1.0, 1.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec1::multiply(&vec2::multiply(2.0))",
                ctx -> {},
                env -> {
                    env.setRootVariable("vec1", vec1);
                    env.setRootVariable("vec2", vec2);
                }
        );
        // vec2 * 2.0 = (2,2,2), vec1 * (2,2,2) = (4,6,8)
        assertEquals("Vector(4.0,6.0,8.0)", result.getInterpretResult().toString());
        assertEquals("Vector(4.0,6.0,8.0)", result.getCompileResult().toString());
    }

    @Test
    void testNestedExtensionCallDoubleResult() {
        MockVector vec1 = new MockVector(2.0, 3.0, 4.0);
        MockVector vec2 = new MockVector(5.0, 6.0, 7.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "d = 0.5\n&vec1::multiply(&vec2::multiply(&d))",
                ctx -> {},
                env -> {
                    env.setRootVariable("vec1", vec1);
                    env.setRootVariable("vec2", vec2);
                }
        );
        // vec2 * 0.5 = (2.5,3.0,3.5), vec1 * (2.5,3.0,3.5) = (5.0, 9.0, 14.0)
        assertEquals("Vector(5.0,9.0,14.0)", result.getInterpretResult().toString());
        assertEquals("Vector(5.0,9.0,14.0)", result.getCompileResult().toString());
    }

    @Test
    void testExtensionArgFromSystemFunction() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::multiply(abs(-2.0))",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // abs(-2.0) = 2.0, vec * 2.0 = (2.0, 4.0, 6.0)
        assertEquals("Vector(2.0,4.0,6.0)", result.getInterpretResult().toString());
        assertEquals("Vector(2.0,4.0,6.0)", result.getCompileResult().toString());
    }

    @Test
    void testExtensionArgFromNestedSystem() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "p = -2.0\nq = 4.0\n&vec::multiply(max(abs(&p), abs(&q)))",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // abs(-2.0) = 2.0, abs(4.0) = 4.0, max = 4.0, vec * 4.0
        assertEquals("Vector(4.0,8.0,12.0)", result.getInterpretResult().toString());
        assertEquals("Vector(4.0,8.0,12.0)", result.getCompileResult().toString());
    }

    @Test
    void testSystemRandomInChainWithLiterals() {
        MockVector vec = new MockVector(1.0, 1.0, 1.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::multiply(random(-1.0, 1.0))",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());
    }

    @Test
    void testSystemRandomInChainNoArgs() {
        MockVector vec = new MockVector(1.0, 1.0, 1.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::multiply(random())",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        assertNotNull(result.getInterpretResult());
        assertNotNull(result.getCompileResult());
    }

    @Test
    void testSystemAbsInChainWithDynamic() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "v = -2.0\n&vec::multiply(abs(&v))",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // abs(-2.0) = 2.0
        assertEquals("Vector(2.0,4.0,6.0)", result.getInterpretResult().toString());
        assertEquals("Vector(2.0,4.0,6.0)", result.getCompileResult().toString());
    }

    @Test
    void testSystemMaxInChainWithDynamic() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 1.5\nb = 2.5\n&vec::multiply(max(&a, &b))",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // max(1.5, 2.5) = 2.5
        assertEquals("Vector(2.5,5.0,7.5)", result.getInterpretResult().toString());
        assertEquals("Vector(2.5,5.0,7.5)", result.getCompileResult().toString());
    }

    @Test
    void testSystemMinInChainWithDynamic() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "a = 1.5\nb = 0.5\n&vec::multiply(min(&a, &b))",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // min(1.5, 0.5) = 0.5
        assertEquals("Vector(0.5,1.0,1.5)", result.getInterpretResult().toString());
        assertEquals("Vector(0.5,1.0,1.5)", result.getCompileResult().toString());
    }

    @Test
    void testSystemRoundInChainDivision() {
        MockVector vec = new MockVector(10.0, 20.0, 30.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "v = 3.7\n&vec::multiply(round(&v) / 10.0)",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // round(3.7) = 4, 4 / 10.0 = 0.4
        assertEquals("Vector(4.0,8.0,12.0)", result.getInterpretResult().toString());
        assertEquals("Vector(4.0,8.0,12.0)", result.getCompileResult().toString());
    }

    @Test
    void testIntWideningToDoubleInExtension() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::multiply(&intVal)",
                ctx -> {},
                env -> {
                    env.setRootVariable("vec", vec);
                    env.setRootVariable("intVal", 5);
                }
        );
        // int 5 → double 5.0, vec * 5.0 = (5.0, 10.0, 15.0)
        assertEquals("Vector(5.0,10.0,15.0)", result.getInterpretResult().toString());
        assertEquals("Vector(5.0,10.0,15.0)", result.getCompileResult().toString());
    }

    @Test
    void testLongWideningToDoubleInExtension() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::multiply(&longVal)",
                ctx -> {},
                env -> {
                    env.setRootVariable("vec", vec);
                    env.setRootVariable("longVal", 5L);
                }
        );
        assertEquals("Vector(5.0,10.0,15.0)", result.getInterpretResult().toString());
        assertEquals("Vector(5.0,10.0,15.0)", result.getCompileResult().toString());
    }

    @Test
    void testFloatWideningToDoubleInExtension() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::multiply(&floatVal)",
                ctx -> {},
                env -> {
                    env.setRootVariable("vec", vec);
                    env.setRootVariable("floatVal", 2.5f);
                }
        );
        assertEquals("Vector(2.5,5.0,7.5)", result.getInterpretResult().toString());
        assertEquals("Vector(2.5,5.0,7.5)", result.getCompileResult().toString());
    }

    @Test
    void testScaleOverloadIntVsDouble() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult resultInt = FluxonTestUtil.runSilent(
                "&vec::scale(3)",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        FluxonTestUtil.TestResult resultDouble = FluxonTestUtil.runSilent(
                "&vec::scale(3.0)",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // 数值结果相同，但应分别选中 scale(I) 和 scale(D)
        assertEquals("Vector(3.0,6.0,9.0)", resultInt.getInterpretResult().toString());
        assertEquals("Vector(3.0,6.0,9.0)", resultInt.getCompileResult().toString());
        assertEquals("Vector(3.0,6.0,9.0)", resultDouble.getInterpretResult().toString());
        assertEquals("Vector(3.0,6.0,9.0)", resultDouble.getCompileResult().toString());
    }

    @Test
    void testScaleOverloadDynamicInt() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::scale(&v)",
                ctx -> {},
                env -> {
                    env.setRootVariable("vec", vec);
                    env.setRootVariable("v", 3);
                }
        );
        assertEquals("Vector(3.0,6.0,9.0)", result.getInterpretResult().toString());
        assertEquals("Vector(3.0,6.0,9.0)", result.getCompileResult().toString());
    }

    @Test
    void testScaleOverloadDynamicDouble() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::scale(&v)",
                ctx -> {},
                env -> {
                    env.setRootVariable("vec", vec);
                    env.setRootVariable("v", 3.0);
                }
        );
        assertEquals("Vector(3.0,6.0,9.0)", result.getInterpretResult().toString());
        assertEquals("Vector(3.0,6.0,9.0)", result.getCompileResult().toString());
    }

    @Test
    void testSafeCallWithNonNull() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec?::multiply(2.0)",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        assertEquals("Vector(2.0,4.0,6.0)", result.getInterpretResult().toString());
        assertEquals("Vector(2.0,4.0,6.0)", result.getCompileResult().toString());
    }

    @Test
    void testSafeCallWithNull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec?::multiply(2.0)",
                ctx -> {},
                env -> env.setRootVariable("vec", null)
        );
        assertNull(result.getInterpretResult());
        assertNull(result.getCompileResult());
    }

    @Test
    void testSafeCallChain() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec?::multiply(2.0)?::multiply(3.0)",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        assertEquals("Vector(6.0,12.0,18.0)", result.getInterpretResult().toString());
        assertEquals("Vector(6.0,12.0,18.0)", result.getCompileResult().toString());
    }

    @Test
    void testArithmeticExprAsArg() {
        MockVector vec = new MockVector(10.0, 20.0, 30.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "v = 10.0\n&vec::multiply(&v / 2.0)",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // 10.0 / 2.0 = 5.0
        assertEquals("Vector(50.0,100.0,150.0)", result.getInterpretResult().toString());
        assertEquals("Vector(50.0,100.0,150.0)", result.getCompileResult().toString());
    }

    @Test
    void testUnaryNegationAsArg() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "v = 2.0\n&vec::multiply(-&v)",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        assertEquals("Vector(-2.0,-4.0,-6.0)", result.getInterpretResult().toString());
        assertEquals("Vector(-2.0,-4.0,-6.0)", result.getCompileResult().toString());
    }

    @Test
    void testTernaryAsArg() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "flag = true\n&vec::multiply(if &flag then 2.0 else 0.5)",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        assertEquals("Vector(2.0,4.0,6.0)", result.getInterpretResult().toString());
        assertEquals("Vector(2.0,4.0,6.0)", result.getCompileResult().toString());
    }

    @Test
    void testElvisAsArg() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::multiply(&?val ?: 2.0)",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // val 未定义，使用默认 2.0
        assertEquals("Vector(2.0,4.0,6.0)", result.getInterpretResult().toString());
        assertEquals("Vector(2.0,4.0,6.0)", result.getCompileResult().toString());
    }

    @Test
    void testComputeWithDynamicDouble() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "compute(\"test\", &d)",
                ctx -> {},
                env -> env.setRootVariable("d", 3.14)
        );
        assertEquals("sd:test,3.14", result.getInterpretResult());
        assertEquals("sd:test,3.14", result.getCompileResult());
    }

    @Test
    void testComputeWithDynamicInt() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "compute(\"test\", &i)",
                ctx -> {},
                env -> env.setRootVariable("i", 42)
        );
        assertEquals("si:test,42", result.getInterpretResult());
        assertEquals("si:test,42", result.getCompileResult());
    }

    @Test
    void testComputeWithDynamicString() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "compute(\"test\", &s)",
                ctx -> {},
                env -> env.setRootVariable("s", "hello")
        );
        assertEquals("ss:test,hello", result.getInterpretResult());
        assertEquals("ss:test,hello", result.getCompileResult());
    }

    @Test
    void testComputeWithLiteralMixed() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("compute(\"test\", 3.14)");
        assertEquals("sd:test,3.14", result.getInterpretResult());
        assertEquals("sd:test,3.14", result.getCompileResult());
    }

    @Test
    void testOverloadNoArgs() {
        // random() 在 :: 链内外都正常
        FluxonTestUtil.TestResult standalone = FluxonTestUtil.runSilent("random()");
        assertNotNull(standalone.getInterpretResult());
        assertNotNull(standalone.getCompileResult());
        MockVector vec = new MockVector(1.0, 1.0, 1.0);
        FluxonTestUtil.TestResult inChain = FluxonTestUtil.runSilent(
                "&vec::multiply(random())",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        assertNotNull(inChain.getInterpretResult());
        assertNotNull(inChain.getCompileResult());
    }

    @Test
    void testChainedSystemCallsBeforeExtension() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "&vec::multiply(abs(-0.5))",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // abs(-0.5) = 0.5
        assertEquals("Vector(0.5,1.0,1.5)", result.getInterpretResult().toString());
        assertEquals("Vector(0.5,1.0,1.5)", result.getCompileResult().toString());
    }

    @Test
    void testMultipleExtensionArgsInSystemFunc() {
        MockVector vec = new MockVector(10.0, 20.0, 30.0);
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "location(&vec.x, &vec.y, &vec.z)",
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        assertEquals("xyz:10.0,20.0,30.0", result.getInterpretResult());
        assertEquals("xyz:10.0,20.0,30.0", result.getCompileResult());
    }

    @Test
    void testSameScriptRepeatedCalls() {
        MockVector vec = new MockVector(1.0, 2.0, 3.0);
        String script = "a = 2.0\nb = 3\n" +
                "r1 = &vec::multiply(&a)\n" +
                "r2 = &vec::scale(&b)\n" +
                "&r1::add(&r2)";
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                script,
                ctx -> {},
                env -> env.setRootVariable("vec", vec)
        );
        // r1 = vec * 2.0 = (2,4,6), r2 = vec * 3 = (3,6,9), r1 + r2 = (5,10,15)
        assertEquals("Vector(5.0,10.0,15.0)", result.getInterpretResult().toString());
        assertEquals("Vector(5.0,10.0,15.0)", result.getCompileResult().toString());
    }
}
