package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.runtime.Function;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 注解解析测试
 * 
 * @author sky
 */
public class AnnotationTest {

    @Test
    public void testSimpleAnnotation() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("@listener\n" +
                "def handleEvent() = {\n" +
                "    print(\"Event handled\")\n" +
                "}");

        Function func = testResult.getCompileEnv().getFunction("handleEvent");
        assertNotNull(func);
        
        List<Annotation> annotations = func.getAnnotations();
        assertEquals(1, annotations.size());
        assertEquals("listener", annotations.get(0).getName());
        assertTrue(annotations.get(0).getAttributes().isEmpty());
    }
    
    @Test
    public void testAnnotationWithAttributes() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("@listener(event = \"onStart\", priority = 10)\n" +
                "def handleStart() = {\n" +
                "    print(\"Start event\")\n" +
                "}"
        );

        Function func = testResult.getCompileEnv().getFunction("handleStart");
        assertNotNull(func);
        
        List<Annotation> annotations = func.getAnnotations();
        assertEquals(1, annotations.size());
        
        Annotation annotation = annotations.get(0);
        assertEquals("listener", annotation.getName());
        assertEquals(2, annotation.getAttributes().size());
        assertNotNull(annotation.getAttributes().get("event"));
        assertNotNull(annotation.getAttributes().get("priority"));
    }
    
    @Test
    public void testMultipleAnnotations() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("@deprecated\n" +
                "@listener(event = \"onClick\")\n" +
                "def clickHandler(x, y) = {\n" +
                "    print(\"Clicked at \" + x + \", \" + y)\n" +
                "}"
        );

        Function func = testResult.getCompileEnv().getFunction("clickHandler");
        assertNotNull(func);
        
        List<Annotation> annotations = func.getAnnotations();
        assertEquals(2, annotations.size());
        assertEquals("deprecated", annotations.get(0).getName());
        assertEquals("listener", annotations.get(1).getName());
    }
    
    @Test
    public void testAsyncFunctionWithAnnotation() {
        FluxonTestUtil.TestResult testResult = FluxonTestUtil.runSilent("@async_handler(timeout = 5000)\n" +
                "async def processAsync() = {\n" +
                "    print(\"Processing async\")\n" +
                "}"
        );
        
        Function func = testResult.getCompileEnv().getFunction("processAsync");
        assertNotNull(func);
        assertTrue(func.isAsync());
        
        List<Annotation> annotations = func.getAnnotations();
        assertEquals(1, annotations.size());
        assertEquals("async_handler", annotations.get(0).getName());
    }
    
    @Test
    public void testAnnotationNotOnFunction() {
        assertThrows(Exception.class, () -> {
            FluxonTestUtil.runSilent("@invalid\n" +
                    "val x = 10");
        });
    }
}