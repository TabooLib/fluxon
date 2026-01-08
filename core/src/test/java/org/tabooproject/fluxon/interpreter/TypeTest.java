package org.tabooproject.fluxon.interpreter;

import org.junit.jupiter.api.Test;
import org.tabooproject.fluxon.FluxonTestUtil;
import org.tabooproject.fluxon.parser.error.ParseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.tabooproject.fluxon.FluxonTestUtil.assertBothEqual;
import static org.tabooproject.fluxon.FluxonTestUtil.runSilent;

public class TypeTest {

    @Test
    public void testNumberCheck() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("isNumber(123)");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testArrayCheck() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("isArray(array([]))");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testListCheck() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("isList([])");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    @Test
    public void testMapCheck() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("isMap([:])");
        assertEquals(true, result.getInterpretResult());
        assertEquals(true, result.getCompileResult());
    }

    // === Is Operator Tests ===

    @Test
    public void testIsOperatorWithStringLowercase() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("\"hello\" is string");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorWithStringUppercase() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("\"hello\" is String");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorWithIntLowercase() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("123 is int");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorWithIntUppercase() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("123 is Int");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorWithListLowercase() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("[1, 2, 3] is list");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorWithListUppercase() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("[1, 2, 3] is List");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorWithMapLowercase() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("[\"a\": 1] is map");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorWithMapUppercase() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("[\"a\": 1] is Map");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorWithFullyQualifiedName() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("\"hello\" is java.lang.String");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorNegativeCase() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("\"hello\" is int");
        FluxonTestUtil.assertBothEqual(false, result);
    }

    @Test
    public void testIsOperatorWithNull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("null is string");
        FluxonTestUtil.assertBothEqual(false, result);
    }

    @Test
    public void testIsOperatorWithLong() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("123L is long");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorWithDouble() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("3.14 is double");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorWithFloat() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("3.14F is float");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorWithBoolean() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent("true is boolean");
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorInConditional() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "x = \"test\"; if &x is string 1 else 0"
        );
        FluxonTestUtil.assertBothEqual(1, result);
    }

    @Test
    public void testIsOperatorInLogicalExpression() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "\"test\" is string && 123 is int"
        );
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorPrecedence() {
        // Test that is has correct precedence
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "1 + 1 is int"
        );
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorWithArrayList() {
        // Test with list alias instead of fully-qualified name
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "myList = new java.util.ArrayList(); &myList is list"
        );
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorInheritance() {
        // ArrayList is also a List
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "myList = new java.util.ArrayList(); &myList is list"
        );
        FluxonTestUtil.assertBothEqual(true, result);
    }

    @Test
    public void testIsOperatorInvalidType() {
        assertThrows(ParseException.class, () -> {
            FluxonTestUtil.runSilent("x = 5; &x is UnknownType");
        });
    }

    @Test
    public void testIsOperatorMultipleChecks() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "def checkType(x) { &x is int }; checkType(2)"
        );
        FluxonTestUtil.assertBothEqual(true, result);
    }

    // === When Expression with IS Tests ===

    @Test
    public void testWhenIsString() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "when (\"hello\") { is string -> \"text\"; else -> \"other\" }"
        );
        FluxonTestUtil.assertBothEqual("text", result);
    }

    @Test
    public void testWhenIsInteger() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "when (42) { is int -> \"number\"; else -> \"other\" }"
        );
        FluxonTestUtil.assertBothEqual("number", result);
    }

    @Test
    public void testWhenIsMultipleBranches() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "when (\"test\") { is int -> \"int\"; is string -> \"string\"; else -> \"other\" }"
        );
        FluxonTestUtil.assertBothEqual("string", result);
    }

    @Test
    public void testWhenIsWithList() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "when ([1, 2, 3]) { is string -> \"text\"; is list -> \"list\"; else -> \"other\" }"
        );
        FluxonTestUtil.assertBothEqual("list", result);
    }

    @Test
    public void testWhenIsWithMap() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "when ([:]) { is list -> \"list\"; is map -> \"map\"; else -> \"other\" }"
        );
        FluxonTestUtil.assertBothEqual("map", result);
    }

    @Test
    public void testWhenIsWithNull() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "when (null) { is string -> \"text\"; else -> \"null\" }"
        );
        FluxonTestUtil.assertBothEqual("null", result);
    }

    @Test
    public void testWhenIsWithFullyQualifiedName() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "when (\"hello\") { is java.lang.String -> \"javaString\"; else -> \"other\" }"
        );
        FluxonTestUtil.assertBothEqual("javaString", result);
    }

    @Test
    public void testWhenIsCaseInsensitive() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "when (123) { is Int -> \"INT\"; else -> \"other\" }"
        );
        FluxonTestUtil.assertBothEqual("INT", result);
    }

    @Test
    public void testWhenIsMixedWithEquals() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "when (42) { 100 -> \"hundred\"; is int -> \"integer\"; else -> \"other\" }"
        );
        FluxonTestUtil.assertBothEqual("integer", result);
    }

    @Test
    public void testWhenIsMixedWithContains() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "when (\"x\") { in [\"a\", \"b\"] -> \"inList\"; is string -> \"text\"; else -> \"other\" }"
        );
        FluxonTestUtil.assertBothEqual("text", result);
    }

    @Test
    public void testWhenIsInFunction() {
        FluxonTestUtil.TestResult result = FluxonTestUtil.runSilent(
            "def describe(obj) { when (&obj) { is string -> \"text\"; is int -> \"number\"; is list -> \"list\"; else -> \"unknown\" } }; describe(\"hello\")"
        );
        FluxonTestUtil.assertBothEqual("text", result);
    }

    @Test
    public void testWhenIsMultiline() {
        FluxonTestUtil.TestResult result = runSilent(
                "obj = \"hello\"\n" +
                        "typeLabel = when (&obj) {\n" +
                        "    is int -> \"integer\"\n" +
                        "    is string -> \"text\"\n" +
                        "    is list -> \"list\"\n" +
                        "    else -> \"unknown\"\n" +
                        "}\n" +
                        "&typeLabel"
        );
        assertBothEqual("text", result);
    }
}
