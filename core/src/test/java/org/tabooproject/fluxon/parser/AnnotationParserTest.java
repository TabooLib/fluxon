package org.tabooproject.fluxon.parser;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.Fluxon;
import org.tabooproject.fluxon.interpreter.UserFunction;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Environment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 注解解析测试
 * 
 * @author sky
 */
public class AnnotationParserTest {

    @Test
    public void testSimpleAnnotation() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Fluxon.eval("@listener\n" +
                "def handleEvent() = {\n" +
                "    print(\"Event handled\")\n" +
                "}", env);
        
        UserFunction func = (UserFunction) env.getFunction("handleEvent");
        assertNotNull(func);
        
        List<Annotation> annotations = func.getAnnotations();
        assertEquals(1, annotations.size());
        assertEquals("listener", annotations.get(0).getName());
        assertTrue(annotations.get(0).getAttributes().isEmpty());
    }
    
    @Test
    public void testAnnotationWithAttributes() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Fluxon.eval("@listener(event = \"onStart\", priority = 10)\n" +
                "def handleStart() = {\n" +
                "    print(\"Start event\")\n" +
                "}", env);
        
        UserFunction func = (UserFunction) env.getFunction("handleStart");
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
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Fluxon.eval("@deprecated\n" +
                "@listener(event = \"onClick\")\n" +
                "def clickHandler(x, y) = {\n" +
                "    print(\"Clicked at \" + x + \", \" + y)\n" +
                "}", env);
        
        UserFunction func = (UserFunction) env.getFunction("clickHandler");
        assertNotNull(func);
        
        List<Annotation> annotations = func.getAnnotations();
        assertEquals(2, annotations.size());
        assertEquals("deprecated", annotations.get(0).getName());
        assertEquals("listener", annotations.get(1).getName());
    }
    
    @Test
    public void testAsyncFunctionWithAnnotation() {
        Environment env = FluxonRuntime.getInstance().newEnvironment();
        Fluxon.eval("@async_handler(timeout = 5000)\n" +
                "async def processAsync() = {\n" +
                "    print(\"Processing async\")\n" +
                "}", env);
        
        UserFunction func = (UserFunction) env.getFunction("processAsync");
        assertNotNull(func);
        assertTrue(func.isAsync());
        
        List<Annotation> annotations = func.getAnnotations();
        assertEquals(1, annotations.size());
        assertEquals("async_handler", annotations.get(0).getName());
    }
    
    @Test
    public void testAnnotationNotOnFunction() {
        assertThrows(Exception.class, () -> {
            Fluxon.eval("@invalid\n" +
                    "val x = 10");
        });
    }
}