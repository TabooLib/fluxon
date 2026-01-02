package org.tabooproject.fluxon.inst;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InjectionRegistry 单元测试。
 */
class InjectionRegistryTest {

    @BeforeEach
    void setUp() {
        InjectionRegistry.getInstance().clear();
        CallbackDispatcher.clearAll();
    }

    @Test
    void testRegisterAndUnregister() {
        // 创建注入规格
        InjectionSpec spec = new InjectionSpec("com/example/Foo", "bar", null, InjectionType.BEFORE);

        // 注册
        String id = InjectionRegistry.getInstance().register(spec);
        assertNotNull(id);
        assertTrue(InjectionRegistry.getInstance().hasInjectionsForClass("com/example/Foo"));

        // 获取规格
        List<InjectionSpec> specs = InjectionRegistry.getInstance().getSpecsForClass("com/example/Foo");
        assertEquals(1, specs.size());
        assertEquals("bar", specs.get(0).getMethodName());

        // 撤销
        boolean removed = InjectionRegistry.getInstance().unregister(id);
        assertTrue(removed);
        assertFalse(InjectionRegistry.getInstance().hasInjectionsForClass("com/example/Foo"));
    }

    @Test
    void testUnregisterByTarget() {
        // 注册多个注入
        InjectionSpec spec1 = new InjectionSpec("com/example/Foo", "bar", null, InjectionType.BEFORE);
        InjectionSpec spec2 = new InjectionSpec("com/example/Foo", "baz", null, InjectionType.REPLACE);

        InjectionRegistry.getInstance().register(spec1);
        InjectionRegistry.getInstance().register(spec2);

        assertEquals(2, InjectionRegistry.getInstance().getSpecsForClass("com/example/Foo").size());

        // 按目标撤销
        boolean removed = InjectionRegistry.getInstance().unregisterByTarget("com/example/Foo", "bar", null);
        assertTrue(removed);
        assertEquals(1, InjectionRegistry.getInstance().getSpecsForClass("com/example/Foo").size());
    }

    @Test
    void testUnregisterNonExistent() {
        // 撤销不存在的注入
        assertFalse(InjectionRegistry.getInstance().unregister("non_existent_id"));
        assertFalse(InjectionRegistry.getInstance().unregisterByTarget("com/example/NonExistent", "foo", null));
    }

    @Test
    void testGetAllSpecs() {
        InjectionSpec spec1 = new InjectionSpec("com/example/Foo", "bar", null, InjectionType.BEFORE);
        InjectionSpec spec2 = new InjectionSpec("com/example/Bar", "test", "(I)V", InjectionType.REPLACE);

        InjectionRegistry.getInstance().register(spec1);
        InjectionRegistry.getInstance().register(spec2);

        Collection<InjectionSpec> allSpecs = InjectionRegistry.getInstance().getAllSpecs();
        assertEquals(2, allSpecs.size());
    }

    @Test
    void testMatchesMethodWithDescriptor() {
        InjectionSpec spec = new InjectionSpec("com/example/Foo", "bar", "(Ljava/lang/String;I)V", InjectionType.BEFORE);

        assertTrue(spec.matchesMethod("bar", "(Ljava/lang/String;I)V"));
        assertFalse(spec.matchesMethod("bar", "(I)V"));
        assertFalse(spec.matchesMethod("baz", "(Ljava/lang/String;I)V"));
    }

    @Test
    void testMatchesMethodWithoutDescriptor() {
        // 无描述符时匹配任意同名方法
        InjectionSpec spec = new InjectionSpec("com/example/Foo", "bar", null, InjectionType.BEFORE);

        assertTrue(spec.matchesMethod("bar", "(Ljava/lang/String;I)V"));
        assertTrue(spec.matchesMethod("bar", "(I)V"));
        assertTrue(spec.matchesMethod("bar", "()V"));
        assertFalse(spec.matchesMethod("baz", "()V"));
    }

    @Test
    void testMultipleInjectionsOnSameMethod() {
        // 同一方法多个 BEFORE 注入
        InjectionSpec spec1 = new InjectionSpec("com/example/Foo", "bar", null, InjectionType.BEFORE);
        InjectionSpec spec2 = new InjectionSpec("com/example/Foo", "bar", null, InjectionType.BEFORE);

        InjectionRegistry.getInstance().register(spec1);
        InjectionRegistry.getInstance().register(spec2);

        List<InjectionSpec> specs = InjectionRegistry.getInstance().getSpecsForClass("com/example/Foo");
        assertEquals(2, specs.size());
    }

    @Test
    void testClearAll() {
        InjectionSpec spec1 = new InjectionSpec("com/example/Foo", "bar", null, InjectionType.BEFORE);
        InjectionSpec spec2 = new InjectionSpec("com/example/Bar", "baz", null, InjectionType.REPLACE);

        InjectionRegistry.getInstance().register(spec1);
        InjectionRegistry.getInstance().register(spec2);

        assertEquals(2, InjectionRegistry.getInstance().getAllSpecs().size());

        InjectionRegistry.getInstance().clear();

        assertEquals(0, InjectionRegistry.getInstance().getAllSpecs().size());
        assertFalse(InjectionRegistry.getInstance().hasInjectionsForClass("com/example/Foo"));
        assertFalse(InjectionRegistry.getInstance().hasInjectionsForClass("com/example/Bar"));
    }

    @Test
    void testInjectionSpecGetTarget() {
        // getTarget() returns dot-separated class name (external format)
        InjectionSpec spec1 = new InjectionSpec("com/example/Foo", "bar", null, InjectionType.BEFORE);
        assertEquals("com.example.Foo::bar", spec1.getTarget());

        InjectionSpec spec2 = new InjectionSpec("com/example/Foo", "bar", "(I)V", InjectionType.BEFORE);
        assertEquals("com.example.Foo::bar(I)V", spec2.getTarget());
    }

    @Test
    void testInjectionSpecIdGeneration() {
        InjectionSpec spec1 = new InjectionSpec("com/example/Foo", "bar", null, InjectionType.BEFORE);
        InjectionSpec spec2 = new InjectionSpec("com/example/Foo", "bar", null, InjectionType.BEFORE);

        // 每个规格应有唯一 ID
        assertNotEquals(spec1.getId(), spec2.getId());
        assertTrue(spec1.getId().startsWith("inj_"));
        assertTrue(spec2.getId().startsWith("inj_"));
    }
}
