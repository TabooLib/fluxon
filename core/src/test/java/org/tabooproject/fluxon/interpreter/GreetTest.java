package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.compiler.FluxonFeatures;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class GreetTest {

    @BeforeEach
    public void setUp() {
        FluxonFeatures.DEFAULT_ALLOW_KETHER_STYLE_CALL = true;
    }

    @Test
    public void testGreetWithoutArgument() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("greet");
            String output = outContent.toString();
            assertTrue(output.contains("你好!"), "Expected '你好!' in output but got: " + output);
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testGreetWithArgument() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("greet \"世界\"");
            String output = outContent.toString();
            assertTrue(output.contains("你好, 世界!"), "Expected '你好, 世界!' in output but got: " + output);
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testGreetWithEnglishName() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("greet \"Alice\"");
            String output = outContent.toString();
            assertTrue(output.contains("你好, Alice!"), "Expected '你好, Alice!' in output but got: " + output);
        } finally {
            System.setOut(originalOut);
        }
    }
}
