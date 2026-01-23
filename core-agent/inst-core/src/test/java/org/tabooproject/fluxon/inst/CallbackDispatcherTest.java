package org.tabooproject.fluxon.inst;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Function;
import org.tabooproject.fluxon.runtime.FunctionContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CallbackDispatcher 单元测试。
 */
class CallbackDispatcherTest {

    @BeforeEach
    void setUp() {
        CallbackDispatcher.clearAll();
    }

    @Test
    void testRegisterAndDispatch() {
        Function mockCallback = mock(Function.class);
        Environment mockEnv = mock(Environment.class);
        doAnswer(inv -> {
            ((FunctionContext<?>) inv.getArgument(0)).setReturnRef("result");
            return null;
        }).when(mockCallback).call(any());

        CallbackDispatcher.register("test_id", mockCallback, mockEnv);
        assertTrue(CallbackDispatcher.hasCallback("test_id"));

        Object[] args = new Object[]{"arg1", 42};
        Object result = CallbackDispatcher.dispatch("test_id", args);

        assertEquals("result", result);
        verify(mockCallback, times(1)).call(any(FunctionContext.class));
    }

    @Test
    void testDispatchNonExistent() {
        Object result = CallbackDispatcher.dispatch("non_existent", new Object[]{});
        assertEquals(CallbackDispatcher.PROCEED, result);
    }

    @Test
    void testDispatchBefore() {
        Function mockCallback = mock(Function.class);
        Environment mockEnv = mock(Environment.class);

        // returnRef = null → dispatch returns PROCEED → dispatchBefore returns true
        doNothing().when(mockCallback).call(any());
        CallbackDispatcher.register("before_id", mockCallback, mockEnv);

        assertTrue(CallbackDispatcher.dispatchBefore("before_id", new Object[]{}));

        // returnRef = SKIP → dispatch returns SKIP → dispatchBefore returns false
        doAnswer(inv -> {
            ((FunctionContext<?>) inv.getArgument(0)).setReturnRef(CallbackDispatcher.SKIP);
            return null;
        }).when(mockCallback).call(any());
        assertFalse(CallbackDispatcher.dispatchBefore("before_id", new Object[]{}));
    }

    @Test
    void testDispatchReplace() {
        Function mockCallback = mock(Function.class);
        Environment mockEnv = mock(Environment.class);
        doAnswer(inv -> {
            ((FunctionContext<?>) inv.getArgument(0)).setReturnRef("replaced");
            return null;
        }).when(mockCallback).call(any());

        CallbackDispatcher.register("replace_id", mockCallback, mockEnv);

        Object result = CallbackDispatcher.dispatchReplace("replace_id", new Object[]{});
        assertEquals("replaced", result);
    }

    @Test
    void testUnregister() {
        Function mockCallback = mock(Function.class);
        Environment mockEnv = mock(Environment.class);

        CallbackDispatcher.register("to_remove", mockCallback, mockEnv);
        assertTrue(CallbackDispatcher.hasCallback("to_remove"));

        CallbackDispatcher.unregister("to_remove");
        assertFalse(CallbackDispatcher.hasCallback("to_remove"));
    }

    @Test
    void testClearAll() {
        Function mockCallback = mock(Function.class);
        Environment mockEnv = mock(Environment.class);

        CallbackDispatcher.register("id1", mockCallback, mockEnv);
        CallbackDispatcher.register("id2", mockCallback, mockEnv);

        assertTrue(CallbackDispatcher.hasCallback("id1"));
        assertTrue(CallbackDispatcher.hasCallback("id2"));

        CallbackDispatcher.clearAll();

        assertFalse(CallbackDispatcher.hasCallback("id1"));
        assertFalse(CallbackDispatcher.hasCallback("id2"));
    }
}
