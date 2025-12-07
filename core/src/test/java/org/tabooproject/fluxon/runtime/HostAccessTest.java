package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.FluxonFeatures;
import org.tabooproject.fluxon.runtime.java.Export;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HostAccessTest
 *
 * @author TheFloodDragon
 * @since 2025/12/6 21:49
 */
public class HostAccessTest {

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

    @BeforeAll
    public static void setup() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();

        // 注册基本测试类
        runtime.registerFunction("test:access", "access", 0, (context) -> ClassToAccess.INSTANCE);
        runtime.getExportRegistry().registerClass(ClassToAccess.class, "test:access");

        // 注册类型继承测试类
        runtime.registerFunction("test:access", "typeTest", 0, (context) -> TypeInheritanceClass.INSTANCE);
        runtime.getExportRegistry().registerClass(TypeInheritanceClass.class, "test:access");

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
        FluxonRuntime.getInstance().registerFunction("test:access", "createArrayList", 0, (context) -> new ArrayList<>());
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

}
