package org.tabooproject.fluxon.runtime.sharing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SharedFunctionRegistryTest {

    @AfterEach
    void cleanup() {
        SharedFunctionRegistry.unregisterAll("testPlugin");
        SharedFunctionRegistry.unregisterAll("pluginA");
        SharedFunctionRegistry.unregisterAll("pluginB");
    }

    @Test
    void testRegisterAndFind() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                SharedFunctionRegistryTest.class, "sampleFunc",
                MethodType.methodType(int.class, int.class, int.class));
        SharedFunctionRegistry.register("testPlugin", "add", mh);
        Object[] entry = SharedFunctionRegistry.find("testPlugin", "add");
        assertNotNull(entry);
        assertEquals(SharedFunctionEntry.CURRENT_VERSION, SharedFunctionEntry.version(entry));
        assertEquals("add", SharedFunctionEntry.name(entry));
        assertEquals("testPlugin", SharedFunctionEntry.owner(entry));
        assertFalse(SharedFunctionEntry.isExtension(entry));
        assertTrue(SharedFunctionEntry.isVersionSupported(entry));
    }

    @Test
    void testFindReturnsNullForMissing() {
        assertNull(SharedFunctionRegistry.find("nonExistent", "nope"));
    }

    @Test
    void testFindAll() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                SharedFunctionRegistryTest.class, "sampleFunc",
                MethodType.methodType(int.class, int.class, int.class));
        SharedFunctionRegistry.register("pluginA", "heal", mh);
        SharedFunctionRegistry.register("pluginB", "heal", mh);
        List<Object[]> results = SharedFunctionRegistry.findAll("heal");
        assertEquals(2, results.size());
    }

    @Test
    void testUnregisterAll() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                SharedFunctionRegistryTest.class, "sampleFunc",
                MethodType.methodType(int.class, int.class, int.class));
        SharedFunctionRegistry.register("testPlugin", "a", mh);
        SharedFunctionRegistry.register("testPlugin", "b", mh);
        assertEquals(2, SharedFunctionRegistry.unregisterAll("testPlugin"));
        assertNull(SharedFunctionRegistry.find("testPlugin", "a"));
    }

    @Test
    void testInvokeThroughHandle() throws Throwable {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                SharedFunctionRegistryTest.class, "sampleFunc",
                MethodType.methodType(int.class, int.class, int.class));
        SharedFunctionRegistry.register("testPlugin", "add", mh);
        Object[] entry = SharedFunctionRegistry.find("testPlugin", "add");
        int result = (int) SharedFunctionEntry.handle(entry).invoke(3, 5);
        assertEquals(8, result);
    }

    @Test
    void testRegisterAndFindExtension() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findVirtual(
                String.class, "length", MethodType.methodType(int.class));
        SharedFunctionRegistry.registerExtension("testPlugin", "len", mh, String.class);
        assertNull(SharedFunctionRegistry.find("testPlugin", "len")); // 不是普通函数
        Object[] entry = SharedFunctionRegistry.findExtension("testPlugin", "len", String.class);
        assertNotNull(entry);
        assertTrue(SharedFunctionEntry.isExtension(entry));
        assertEquals(String.class, SharedFunctionEntry.extensionTarget(entry));
    }

    @Test
    void testGetOwners() throws Exception {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                SharedFunctionRegistryTest.class, "sampleFunc",
                MethodType.methodType(int.class, int.class, int.class));
        SharedFunctionRegistry.register("pluginA", "f1", mh);
        SharedFunctionRegistry.register("pluginB", "f2", mh);
        List<String> owners = SharedFunctionRegistry.getOwners();
        assertTrue(owners.contains("pluginA"));
        assertTrue(owners.contains("pluginB"));
    }

    public static int sampleFunc(int a, int b) {
        return a + b;
    }
}
