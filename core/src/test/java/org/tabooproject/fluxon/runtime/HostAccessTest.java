package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.compiler.FluxonFeatures;
import org.tabooproject.fluxon.runtime.java.Export;
import org.tabooproject.fluxon.runtime.java.Optional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

/**
 * HostAccessTest
 *
 * @author TheFloodDragon
 * @since 2025/12/6 21:49
 */
public class HostAccessTest {

    public enum TestReason {
        MECH,
        SCRIPT
    }

    public static class ClassToAccess {

        public static final ClassToAccess INSTANCE = new ClassToAccess();

        // 无参方法
        @Export
        public String method() {
            return "No arguments method called";
        }

        // 单参方法
        @Export
        public String method(String arg) {
            return "Single argument method called with arg: " + arg;
        }

        // 双参方法
        @Export
        public int method(String arg, int num) {
            return arg.hashCode() + num;
        }


        // 其他方法
        @Export
        public String other(String arg) {
            return "Other method called with arg: " + arg;
        }

        @Export
        public String boolArg(boolean value) {
            return "Boolean argument method called with arg: " + value;
        }

    }

    /**
     * 用于测试类型继承关系的类
     */
    public static class TypeInheritanceClass {

        public static final TypeInheritanceClass INSTANCE = new TypeInheritanceClass();

        // 接受 CharSequence（更通用）
        @Export
        public String process(CharSequence cs) {
            return "CharSequence: " + cs;
        }

        // 接受 String（更具体）
        @Export
        public String process(String str) {
            return "String: " + str;
        }

        // 接受 Number（更通用）
        @Export
        public String calculate(Number num) {
            return "Number: " + num;
        }

        // 接受 Integer（更具体）
        @Export
        public String calculate(Integer num) {
            return "Integer: " + num;
        }

        // 接受 List（接口）
        @Export
        public String handleList(List<?> list) {
            return "List: size=" + list.size();
        }

        // 接受 ArrayList（具体实现）
        @Export
        public String handleList(ArrayList<?> list) {
            return "ArrayList: size=" + list.size();
        }

        // 测试 null 参数：接受 Object
        @Export
        public String acceptNull(Object obj) {
            return obj == null ? "null received" : "Object: " + obj;
        }

        // 测试 null 参数：接受 String
        @Export
        public String acceptNull(String str) {
            return str == null ? "null String received" : "String: " + str;
        }
    }

    public static class FastPathClass {

        public static final FastPathClass INSTANCE = new FastPathClass();

        public String last = "";

        @Export
        public int add(int left, int right) {
            return left + right;
        }

        @Export
        public double scale(double value, float factor) {
            return value * factor;
        }

        @Export
        public boolean flag(boolean value) {
            return value;
        }

        @Export
        public String join(String prefix, int count, boolean enabled) {
            return prefix + ":" + count + ":" + enabled;
        }

        @Export
        public String optional(String prefix, @Optional int count) {
            return prefix + ":" + count;
        }

        @Export
        public void mark(String value) {
            last = value;
        }

        @Export
        public String last() {
            return last;
        }

        @Export
        public String reason(TestReason reason) {
            return reason.name();
        }
    }

    public static class EnumFunctionClass {

        @FluxonFunction(value = "enumReason", namespace = "test:access")
        public static String enumReason(TestReason reason) {
            return reason.name();
        }
    }

    /**
     * 验证 @Export 方法能否接收脚本传入的 Fluxon lambda（Function 参数）
     */
    public static class ExportFunctionParamClass {

        public static final ExportFunctionParamClass INSTANCE = new ExportFunctionParamClass();

        @Export
        public int applyWithLambda(Function fn) {
            Objects.requireNonNull(fn);
            FunctionContextPool pool = FunctionContextPool.local();
            Environment env = FluxonRuntime.getInstance().newEnvironment();
            NativeFunction<?> host = new NativeFunction<>("exportFnHost", FunctionSignature.returns(Type.OBJECT).noParams(), c -> {});
            FunctionContext<?> seed = pool.borrow(host, null, 0, env);
            try (FunctionContext<?> ctx = pool.borrowCopy(seed, null)) {
                ctx.updateRefs(5);
                fn.call(ctx);
                Object result = ctx.getReturnRef();
                if (result instanceof Number) {
                    return ((Number) result).intValue();
                }
                return 0;
            }
        }

        @Export
        public List<Object> mapWithLambda(List<?> items, Function fn) {
            Objects.requireNonNull(items);
            Objects.requireNonNull(fn);
            List<Object> out = new ArrayList<>();
            FunctionContextPool pool = FunctionContextPool.local();
            Environment env = FluxonRuntime.getInstance().newEnvironment();
            NativeFunction<?> host = new NativeFunction<>("exportMapHost", FunctionSignature.returns(Type.OBJECT).noParams(), c -> {});
            FunctionContext<?> seed = pool.borrow(host, null, 0, env);
            try (FunctionContext<?> ctx = pool.borrowCopy(seed, null)) {
                int index = 0;
                for (Object item : items) {
                    ctx.updateRefs(item, index);
                    fn.call(ctx);
                    out.add(ctx.getReturnRef());
                    index++;
                }
            }
            return out;
        }
    }

    @BeforeAll
    public static void setup() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();

        // 注册基本测试类
        runtime.registerFunction("test:access", "access", returns(Type.OBJECT).noParams(), (context) -> context.setReturnRef(ClassToAccess.INSTANCE));
        runtime.getExportRegistry().registerClass(ClassToAccess.class, "test:access");

        // 注册类型继承测试类
        runtime.registerFunction("test:access", "typeTest", returns(Type.OBJECT).noParams(), (context) -> context.setReturnRef(TypeInheritanceClass.INSTANCE));
        runtime.getExportRegistry().registerClass(TypeInheritanceClass.class, "test:access");

        // 注册非重载导出类，覆盖 ClassBridge 按索引直连的热路径。
        runtime.registerFunction("test:access", "fastAccess", returns(Type.OBJECT).noParams(), (context) -> context.setReturnRef(FastPathClass.INSTANCE));
        runtime.getExportRegistry().registerClass(FastPathClass.class, "test:access");
        runtime.registerFunction("test:access", "lambdaAccess", returns(Type.OBJECT).noParams(), (context) -> context.setReturnRef(ExportFunctionParamClass.INSTANCE));
        runtime.getExportRegistry().registerClass(ExportFunctionParamClass.class, "test:access");
        FluxonFunctionScanner.register(runtime, EnumFunctionClass.class);

        // 自动导入
        FluxonFeatures.DEFAULT_PACKAGE_AUTO_IMPORT.add("test:access");
    }

    @Test
    public void testOverrideExport() {
        // 测试调用
        Object noArgResult = Fluxon.eval("access :: method()");
        assertEquals("No arguments method called", noArgResult);
        Object singleArgResult = Fluxon.eval("access :: method('I\\'m a arg')");
        assertEquals("Single argument method called with arg: I'm a arg", singleArgResult);
        Object doubleArgResult = Fluxon.eval("access :: method('test', 42)");
        assertEquals("test".hashCode() + 42, doubleArgResult);
    }

    @Test
    public void testExportBooleanArgument() {
        Object trueResult = Fluxon.eval("access :: boolArg(true)");
        assertEquals("Boolean argument method called with arg: true", trueResult);
        Object falseResult = Fluxon.eval("access :: boolArg(false)");
        assertEquals("Boolean argument method called with arg: false", falseResult);
    }

    @Test
    public void testNonOverloadedExportFastPath() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "fastAccess :: mark('ready')\n" +
                        "[fastAccess :: add(2, 3), fastAccess :: scale(2.5, 4.0), fastAccess :: flag(true), fastAccess :: join('v', 7, false), fastAccess :: optional('x'), fastAccess :: last()]",
                "TestNonOverloadedExportFastPath"
        );
        assertEquals("[5, 10.0, true, v:7:false, x:0, ready]", String.valueOf(result.getInterpretResult()));
        assertEquals("[5, 10.0, true, v:7:false, x:0, ready]", String.valueOf(result.getCompileResult()));
    }

    @Test
    public void testStringLiteralConvertsToEnumParameter() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
                "[fastAccess :: reason(MECH), enumReason(SCRIPT)]",
                "TestStringLiteralConvertsToEnumParameter"
        );
        assertEquals("[MECH, SCRIPT]", String.valueOf(result.getInterpretResult()));
        assertEquals("[MECH, SCRIPT]", String.valueOf(result.getCompileResult()));
    }

    @Test
    public void testTypeSpecificityWithString() {
        // String 应该优先匹配 String 重载，而非 CharSequence
        Object result = Fluxon.eval("typeTest :: process('hello')");
        assertEquals("String: hello", result);
    }

    @Test
    public void testTypeSpecificityWithInteger() {
        // Integer 应该优先匹配 Integer 重载，而非 Number
        Object result = Fluxon.eval("typeTest :: calculate(42)");
        assertEquals("Integer: 42", result);
    }

    @Test
    public void testTypeSpecificityWithArrayList() {
        // 传入 ArrayList，应优先匹配 ArrayList 重载
        FluxonRuntime.getInstance().registerFunction("test:access", "createArrayList", returns(Type.OBJECT).noParams(), (context) -> context.setReturnRef(new ArrayList<>()));
        Object result = Fluxon.eval("typeTest :: handleList(createArrayList())");
        assertEquals("ArrayList: size=0", result);
    }

    @Test
    public void testNullParameter() {
        // null 参数应该能够匹配接受 Object 的方法
        Object result = Fluxon.eval("typeTest :: acceptNull(null)");
        // null 可以匹配 String 或 Object，根据特异性排序 String 更具体
        assertEquals("null String received", result);
    }

    @Test
    public void testExportAcceptsFluxonFunctionParameter() {
        // @Export 可声明 Function 参数并由桥接传入；宿主侧 call 前须用 updateRefs 绑定 || 的 it（与 ExtensionBuilder.forEachElement 一致）
        Object squared = Fluxon.eval("lambdaAccess :: applyWithLambda(|| &it * &it)");
        assertEquals(25, squared);
        FluxonTestUtil.TestResult mapped = FluxonTestUtil.runSilent(
                "lambdaAccess :: mapWithLambda([1, 2, 3], || &it + 10)",
                "TestExportAcceptsFluxonFunctionParameter"
        );
        assertEquals("[11, 12, 13]", String.valueOf(mapped.getInterpretResult()));
        assertEquals("[11, 12, 13]", String.valueOf(mapped.getCompileResult()));
    }

}
