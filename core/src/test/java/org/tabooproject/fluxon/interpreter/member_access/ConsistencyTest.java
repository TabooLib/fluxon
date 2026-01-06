package org.tabooproject.fluxon.interpreter.member_access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.runtime.java.ClassBridge;
import org.tabooproject.fluxon.runtime.java.ExportRegistry;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.type.TestObject;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 解释执行与编译执行一致性测试
 * 确保两种执行模式产生相同的结果
 *
 * @author sky
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ConsistencyTest extends MemberAccessTestBase {

    // ========== 字段访问一致性 ==========

    @Test
    public void testFieldAccessConsistency() throws Exception {
        String[] sources = {
            "&obj.publicField",
            "&obj.intField",
            "&obj.booleanField",
            "&obj.doubleField",
            "&obj.longField",
            "&obj.boxedIntField",
            "&obj.staticField",
            "&obj.finalField"
        };

        for (String source : sources) {
            Object interpretResult = interpret(source);
            Object compileResult = compile(source);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }

    @Test
    public void testNullableFieldConsistency() throws Exception {
        // nullable field 返回 null
        Object interpretResult = interpret("&obj.nullableField");
        Object compileResult = compile("&obj.nullableField");
        assertEquals(interpretResult, compileResult);
        assertNull(interpretResult);
    }

    @Test
    public void testCollectionFieldConsistency() throws Exception {
        Object interpretResult = interpret("&obj.listField");
        Object compileResult = compile("&obj.listField");
        assertTrue(interpretResult instanceof List);
        assertTrue(compileResult instanceof List);
        assertEquals(((List<?>) interpretResult).size(), ((List<?>) compileResult).size());
    }

    // ========== 方法调用一致性 ==========

    @Test
    public void testNoArgsMethodConsistency() throws Exception {
        String[] sources = {
            "&obj.getName()",
            "&obj.getNumber()",
            "&obj.isEnabled()",
            "&obj.getDouble()",
            "&obj.getLong()",
            "&obj.noArgs()"
        };

        for (String source : sources) {
            Object interpretResult = interpret(source);
            Object compileResult = compile(source);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }

    @Test
    public void testArgsMethodConsistency() throws Exception {
        String[] sources = {
            "&obj.concat('hello', 'world')",
            "&obj.add(10, 20)",
            "&obj.processBoolean(true)",
            "&obj.processDouble(3.14)",
            "&obj.oneArg('test')",
            "&obj.twoArgs('a', 'b')",
            "&obj.threeArgs('x', 'y', 'z')"
        };

        for (String source : sources) {
            Object interpretResult = interpret(source);
            Object compileResult = compile(source);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }

    @Test
    public void testOverloadMethodConsistency() throws Exception {
        String[] sources = {
            "&obj.overload()",
            "&obj.overload(42)",
            "&obj.overload('hello')",
            "&obj.overload(1, 2)",
            "&obj.overload('a', 1)",
            "&obj.overload(1, 'b')",
            "&obj.overload(1, 2, 3)",
            "&obj.process('test')",
            "&obj.process(42)",
            "&obj.process('a', 'b')"
        };

        for (String source : sources) {
            Object interpretResult = interpret(source);
            Object compileResult = compile(source);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }

    @Test
    public void testVoidMethodConsistency() throws Exception {
        Object interpretResult = interpret("&obj.voidMethod()");
        Object compileResult = compile("&obj.voidMethod()");
        assertNull(interpretResult);
        assertNull(compileResult);
    }

    @Test
    public void testNullReturnConsistency() throws Exception {
        Object interpretResult = interpret("&obj.getNullValue()");
        Object compileResult = compile("&obj.getNullValue()");
        assertNull(interpretResult);
        assertNull(compileResult);
    }

    // ========== 链式调用一致性 ==========

    @Test
    public void testChainedCallConsistency() throws Exception {
        String[] sources = {
            "&obj.getSelf().publicField",
            "&obj.getSelf().getName()",
            "&obj.getSelf().getSelf().intField",
            "&obj.getSelf().getSelf().getSelf().getName()",
            "&obj.createNested().publicField"
        };

        for (String source : sources) {
            Object interpretResult = interpret(source);
            Object compileResult = compile(source);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }

    @Test
    public void testDotWithContextCallConsistency() throws Exception {
        String[] sources = {
            "&obj.publicField::split('-')::size()",
            "&obj.getName()::split('-')::size()",
            "&obj.getSelf().publicField::split('-')::size()"
        };

        for (String source : sources) {
            Object interpretResult = interpret(source);
            Object compileResult = compile(source);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }

    // ========== 循环中的一致性 ==========

    @Test
    public void testLoopFieldAccessConsistency() throws Exception {
        String source =
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 5) { " +
            "  sum = &sum + &obj.intField; " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        
        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
        assertEquals(210, interpretResult); // 42 * 5
    }

    @Test
    public void testLoopMethodCallConsistency() throws Exception {
        String source =
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 10) { " +
            "  sum = &sum + &obj.add(1, 2); " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        
        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
        assertEquals(30, interpretResult); // 3 * 10
    }

    @Test
    public void testLoopChainedCallConsistency() throws Exception {
        String source =
            "sum = 0; " +
            "i = 0; " +
            "while (&i < 3) { " +
            "  sum = &sum + &obj.getSelf().getSelf().add(&i, &i); " +
            "  i = &i + 1; " +
            "}; " +
            "&sum";
        
        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
        assertEquals(6, interpretResult); // 0+0 + 1+1 + 2+2 = 0 + 2 + 4 = 6
    }

    // ========== 多实例一致性 ==========

    @Test
    public void testMultipleInstancesConsistency() throws Exception {
        String source =
            "a = &obj1.publicField; " +
            "b = &obj2.publicField; " +
            "&a + '-' + &b";

        TestObject obj1 = new TestObject();
        obj1.publicField = "first";
        TestObject obj2 = new TestObject();
        obj2.publicField = "second";

        Object interpretResult = interpret(source, env -> {
            env.defineRootVariable("obj1", obj1);
            env.defineRootVariable("obj2", obj2);
        });

        Object compileResult = compile(source, env -> {
            env.defineRootVariable("obj1", obj1);
            env.defineRootVariable("obj2", obj2);
        });

        assertEquals(interpretResult, compileResult);
        assertEquals("first-second", interpretResult);
    }

    @Test
    public void testPolymorphicCallConsistency() throws Exception {
        String source =
            "results = []; " +
            "for o in &objects { " +
            "  results = &results + [&o.getName()]; " +
            "}; " +
            "&results::size()";

        List<TestObject> objects = Arrays.asList(new TestObject(), new TestObject(), new TestObject());

        Object interpretResult = interpret(source, env -> {
            env.defineRootVariable("objects", objects);
        });

        Object compileResult = compile(source, env -> {
            env.defineRootVariable("objects", objects);
        });

        assertEquals(interpretResult, compileResult);
        assertEquals(3, interpretResult);
    }

    // ========== ClassBridge 一致性 ==========

    @Test
    public void testClassBridgeConsistency() throws Exception {
        // 注册 ClassBridge
        ExportRegistry registry = new ExportRegistry(FluxonRuntime.getInstance());
        registry.registerClass(TestObject.class);

        ClassBridge bridge = ExportRegistry.getClassBridge(TestObject.class);
        assertNotNull(bridge);

        String[] sources = {
            "&obj.bridgedMethod()",
            "&obj.bridgedMethodWithArg('test')",
            "&obj.bridgedAdd(10, 20)"
        };

        for (String source : sources) {
            Object interpretResult = interpret(source);
            Object compileResult = compile(source);
            assertEquals(interpretResult, compileResult, "Mismatch for: " + source);
        }
    }

    @Test
    public void testMixedBridgeAndReflectionConsistency() throws Exception {
        ExportRegistry registry = new ExportRegistry(FluxonRuntime.getInstance());
        registry.registerClass(TestObject.class);

        String source =
            "bridged = &obj.bridgedAdd(5, 5); " +
            "reflect = &obj.add(5, 5); " +
            "&bridged + &reflect";

        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
        assertEquals(1020, interpretResult); // 1010 + 10
    }

    // ========== 复杂表达式一致性 ==========

    @Test
    public void testComplexExpressionConsistency() throws Exception {
        String source =
            "x = &obj.add(1, 2); " +
            "y = &obj.getSelf().add(&x, &x); " +
            "z = &obj.concat(&obj.getName(), '-suffix'); " +
            "&y";

        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
        assertEquals(6, interpretResult); // add(3, 3) = 6
    }

    @Test
    public void testConditionalWithReflectionConsistency() throws Exception {
        String source = "if &obj.isEnabled() { &obj.getName() } else { 'disabled' }";
        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
        assertEquals("test-object", interpretResult);
    }

    @Test
    public void testArithmeticWithReflectionConsistency() throws Exception {
        String source = "&obj.add(10, 20) + &obj.getNumber() * 2";
        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
        assertEquals(230, interpretResult); // 30 + 100*2 = 230
    }

    // ========== 边界值一致性 ==========

    @Test
    public void testLargeNumberConsistency() throws Exception {
        String source = "&obj.processLong(9223372036854775807)"; // Long.MAX_VALUE
        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
    }

    @Test
    public void testEmptyStringConsistency() throws Exception {
        String source = "&obj.concat('', '')";
        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
        assertEquals("", interpretResult);
    }

    @Test
    public void testSpecialCharactersConsistency() throws Exception {
        String source = "&obj.echoSpecial('hello\\nworld')";
        Object interpretResult = interpret(source);
        Object compileResult = compile(source);
        assertEquals(interpretResult, compileResult);
    }
}
