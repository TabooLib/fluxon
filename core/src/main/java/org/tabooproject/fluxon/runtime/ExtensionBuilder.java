package org.tabooproject.fluxon.runtime;

import java.util.List;

public class ExtensionBuilder<Target> {

    private final FluxonRuntime runtime;
    private final Class<Target> extensionClass;
    private final String namespace;

    public ExtensionBuilder(FluxonRuntime runtime, Class<Target> extensionClass, String namespace) {
        this.runtime = runtime;
        this.extensionClass = extensionClass;
        this.namespace = namespace;
    }

    public ExtensionBuilder<Target> function(String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        runtime.registerExtensionFunction(extensionClass, namespace, name, paramCount, implementation);
        return this;
    }

    public ExtensionBuilder<Target> function(String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        runtime.registerExtensionFunction(extensionClass, namespace, name, paramCounts, implementation);
        return this;
    }

    public ExtensionBuilder<Target> asyncFunction(String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        runtime.registerAsyncExtensionFunction(extensionClass, namespace, name, paramCount, implementation);
        return this;
    }

    public ExtensionBuilder<Target> asyncFunction(String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        runtime.registerAsyncExtensionFunction(extensionClass, namespace, name, paramCounts, implementation);
        return this;
    }

    public ExtensionBuilder<Target> syncFunction(String name, int paramCount, NativeFunction.NativeCallable<Target> implementation) {
        runtime.registerSyncExtensionFunction(extensionClass, namespace, name, paramCount, implementation);
        return this;
    }

    public ExtensionBuilder<Target> syncFunction(String name, List<Integer> paramCounts, NativeFunction.NativeCallable<Target> implementation) {
        runtime.registerSyncExtensionFunction(extensionClass, namespace, name, paramCounts, implementation);
        return this;
    }

    public Class<Target> getExtensionClass() {
        return extensionClass;
    }

    public String getNamespace() {
        return namespace;
    }
}
