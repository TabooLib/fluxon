package org.tabooproject.fluxon.interpreter;

import org.tabooproject.fluxon.FluxonRuntimeTest;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.util.Comparator;

public class ExtensionFunctionTest {

    public static void main(String[] args) {
        FluxonRuntimeTest.registerTestFunctions();
        FluxonRuntime.getInstance().getExtensionFunctions()
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(a -> a.getValue().size()))
                .forEach(entry -> {
                    System.out.println(entry.getKey() + ": " + entry.getValue().size());
                });
    }
}
