package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.compiler.FluxonFeatures;
import org.tabooproject.fluxon.runtime.java.Export;

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

    @Test
    public void testOverrideExport() {
        FluxonRuntime runtime = FluxonRuntime.getInstance();
        // 获取访问类对象
        runtime.registerFunction("test:access", "access", 0, (context) -> ClassToAccess.INSTANCE);
        // 注册访问类对象实例
        runtime.getExportRegistry().registerClass(ClassToAccess.class, "test:access");
        // 自动导入
        FluxonFeatures.DEFAULT_PACKAGE_AUTO_IMPORT.add("test:access");

        // 测试调用
        Object noArgResult = Fluxon.eval("access :: method()");
        assertEquals("No arguments method called", noArgResult);
        Object singleArgResult = Fluxon.eval("access :: method('I\\'m a arg')");
        assertEquals("Single argument method called with arg: I'm a arg", singleArgResult);
        Object doubleArgResult = Fluxon.eval("access :: method('test', 42)");
        assertEquals("test".hashCode() + 42, doubleArgResult);
    }

}
