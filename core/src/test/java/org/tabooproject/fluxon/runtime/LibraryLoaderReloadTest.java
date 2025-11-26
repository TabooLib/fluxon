package org.tabooproject.fluxon.runtime;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.parser.error.FunctionNotFoundException;
import org.tabooproject.fluxon.runtime.library.LibraryLoader;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class LibraryLoaderReloadTest {

    private final FluxonRuntime runtime = FluxonRuntime.getInstance();

    @Test
    void unloadsAndReloadsLibraryFunctions() {
        LibraryLoader loader = new LibraryLoader(runtime);
        Path v1 = Paths.get("src/test/fs/library_reload_v1.fs");
        Path v2 = Paths.get("src/test/fs/library_reload_v2.fs");

        try (LibraryLoader.LibraryLoadResult first = loader.load(v1)) {
            assertTrue(runtime.getSystemFunctions().containsKey("reloadValue"));
            assertEquals(1, Fluxon.eval("reloadValue()"));
        }

        assertFalse(runtime.getSystemFunctions().containsKey("reloadValue"));
        assertThrows(FunctionNotFoundException.class, () -> Fluxon.eval("reloadValue()"));

        try (LibraryLoader.LibraryLoadResult second = loader.load(v2)) {
            assertTrue(runtime.getSystemFunctions().containsKey("reloadValue"));
            assertEquals(2, Fluxon.eval("reloadValue()"));
        }
    }

    @Test
    void unloadsExtensionFunctions() {
        LibraryLoader loader = new LibraryLoader(runtime);
        Path bind = Paths.get("src/test/fs/library_bind.fs");

        try (LibraryLoader.LibraryLoadResult result = loader.load(bind)) {
            assertTrue(runtime.getExtensionFunctions().containsKey("extConcat"));
            assertEquals("Fluxon", Fluxon.eval("\"Flux\" :: extConcat(\"on\")"));
        }

        assertFalse(runtime.getExtensionFunctions().containsKey("extConcat"));
        assertThrows(FunctionNotFoundException.class, () -> Fluxon.eval("\"Flux\" :: extConcat(\"on\")"));
    }

    @Test
    void unloadManagedResultsCleansUpTracked() {
        LibraryLoader loader = new LibraryLoader(runtime);
        Path v1 = Paths.get("src/test/fs/library_reload_v1.fs");

        loader.load(v1);
        assertTrue(runtime.getSystemFunctions().containsKey("reloadValue"));
        loader.unloadManagedResults();
        assertFalse(runtime.getSystemFunctions().containsKey("reloadValue"));
        assertTrue(loader.getManagedResults().isEmpty());
    }
}
