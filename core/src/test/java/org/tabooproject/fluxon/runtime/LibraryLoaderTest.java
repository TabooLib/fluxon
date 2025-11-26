package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.runtime.library.LibraryLoader;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LibraryLoaderTest {

    private final FluxonRuntime runtime = FluxonRuntime.getInstance();

    @Test
    void loadsApiFunctionsIntoRuntime() {
        LibraryLoader loader = new LibraryLoader(runtime);
        try (LibraryLoader.LibraryLoadResult result = loader.load(Paths.get("src/test/fs/library.fs"))) {

            // @api 函数已注册
            assertTrue(runtime.getSystemFunctions().containsKey("libHello"));
            assertTrue(runtime.getSystemFunctions().containsKey("libAdd"));
            // 未标记为 @api 的函数不会注册
            assertFalse(runtime.getSystemFunctions().containsKey("hiddenHelper"));

            List<?> exported = result.getExportedFunctions();
            assertEquals(2, exported.size());

            Function libHello = runtime.getSystemFunctions().get("libHello");
            Environment env = runtime.newEnvironment();
            Object direct = libHello.call(new FunctionContext<>(libHello, null, new Object[]{"Fluxon"}, env));
            assertEquals("Hello, Fluxon", direct);

            // 通过运行时调用导出的函数
            Object greeting = Fluxon.eval("libHello(\"Fluxon\")");
            assertEquals("Hello, Fluxon", greeting);
            Object sum = Fluxon.eval("libAdd(1, 2)");
            assertEquals(3, sum);
        }
    }
}
