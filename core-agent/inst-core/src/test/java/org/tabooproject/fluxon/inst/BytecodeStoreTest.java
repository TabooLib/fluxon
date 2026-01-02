package org.tabooproject.fluxon.inst;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.inst.bytecode.BytecodeStore;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OriginalBytecodeStore 单元测试。
 */
class BytecodeStoreTest {

    @BeforeEach
    void setUp() {
        BytecodeStore.getInstance().clear();
    }

    @Test
    void testStoreAndGet() {
        byte[] bytecode = new byte[]{1, 2, 3, 4, 5};
        
        BytecodeStore.getInstance().store("com/example/Foo", bytecode);
        
        byte[] retrieved = BytecodeStore.getInstance().get("com/example/Foo");
        assertNotNull(retrieved);
        assertArrayEquals(bytecode, retrieved);
    }

    @Test
    void testStoreOnlyOnce() {
        byte[] original = new byte[]{1, 2, 3};
        byte[] modified = new byte[]{4, 5, 6};

        BytecodeStore.getInstance().store("com/example/Foo", original);
        BytecodeStore.getInstance().store("com/example/Foo", modified);

        // 应该保留第一次存储的值
        byte[] retrieved = BytecodeStore.getInstance().get("com/example/Foo");
        assertArrayEquals(original, retrieved);
    }

    @Test
    void testGetNonExistent() {
        byte[] retrieved = BytecodeStore.getInstance().get("com/example/NonExistent");
        assertNull(retrieved);
    }

    @Test
    void testRemove() {
        byte[] bytecode = new byte[]{1, 2, 3};
        
        BytecodeStore.getInstance().store("com/example/Foo", bytecode);
        assertNotNull(BytecodeStore.getInstance().get("com/example/Foo"));

        BytecodeStore.getInstance().remove("com/example/Foo");
        assertNull(BytecodeStore.getInstance().get("com/example/Foo"));
    }

    @Test
    void testClear() {
        BytecodeStore.getInstance().store("com/example/Foo", new byte[]{1});
        BytecodeStore.getInstance().store("com/example/Bar", new byte[]{2});

        BytecodeStore.getInstance().clear();

        assertNull(BytecodeStore.getInstance().get("com/example/Foo"));
        assertNull(BytecodeStore.getInstance().get("com/example/Bar"));
    }

    @Test
    void testHas() {
        assertFalse(BytecodeStore.getInstance().has("com/example/Foo"));

        BytecodeStore.getInstance().store("com/example/Foo", new byte[]{1});
        assertTrue(BytecodeStore.getInstance().has("com/example/Foo"));

        BytecodeStore.getInstance().remove("com/example/Foo");
        assertFalse(BytecodeStore.getInstance().has("com/example/Foo"));
    }
}
