package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.runtime.library.LibraryLoader;

import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LibraryLoaderExtensionBindTest {

    private final FluxonRuntime runtime = FluxonRuntime.getInstance();

    @Test
    void registersApiFunctionsAsExtensionsUsingEnvTarget() {
        LibraryLoader loader = new LibraryLoader(runtime);
        try (LibraryLoader.LibraryLoadResult ignored = loader.load(Paths.get("src/test/fs/library_bind.fs"))) {

            Map<String, Map<Class<?>, Function>> extensionFunctions = runtime.getExtensionFunctions();
            assertTrue(extensionFunctions.containsKey("extConcat"));
            assertTrue(extensionFunctions.containsKey("extTimes"));

            Object concat = Fluxon.eval("\"Flux\" :: extConcat(\"on\")");
            assertEquals("Fluxon", concat);

            Object multiplied = Fluxon.eval("2 :: extTimes(4)");
            assertEquals(8, multiplied);
        }
    }

    @Test
    void thisFunctionReturnsCurrentTarget() {
        Object self = Fluxon.eval("\"hello\" :: { &?this }");
        assertEquals("hello", self);
    }
}
