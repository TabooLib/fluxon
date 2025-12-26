package org.tabooproject.fluxon.runtime;

import org.tabooproject.fluxon.type.TestRuntime;

import java.util.Comparator;

public class ExtensionFunctionTest {

    public static void main(String[] args) {
        TestRuntime.registerTestFunctions();
        FluxonRuntime.getInstance().getExtensionFunctions()
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(a -> a.getValue().size()))
                .forEach(entry -> {
                    System.out.println(entry.getKey() + ": " + entry.getValue().size());
                });
    }
}
