package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class ExtensionPath {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Path.class, "fs:io")
                .function("name", returns(Type.OBJECT).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    Path fileName = path.getFileName();
                    context.setReturnRef(fileName != null ? fileName.toString() : "");
                })
                .function("parent", returns(Type.OBJECT).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.getParent());
                })
                .function("root", returns(Type.OBJECT).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.getRoot());
                })
                .function("resolve", returns(Type.OBJECT).params(Type.OBJECT), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    String other = Coerce.asString(context.getRef(0)).orElse("");
                    context.setReturnRef(path.resolve(other));
                })
                .function("relativize", returns(Type.OBJECT).params(Type.OBJECT), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    Object arg = context.getRef(0);
                    Path other = arg instanceof Path ? (Path) arg : Paths.get(arg.toString());
                    context.setReturnRef(path.relativize(other));
                })
                .function("normalize", returns(Type.OBJECT).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.normalize());
                })
                .function("toAbsolutePath", returns(Type.OBJECT).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.toAbsolutePath());
                })
                .function("toRealPath", returns(Type.OBJECT).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    try {
                        context.setReturnRef(path.toRealPath());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to get real path: " + e.getMessage(), e);
                    }
                })
                .function("toFile", returns(Type.OBJECT).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.toFile());
                })
                .function("exists", returns(Type.Z).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Files.exists(path));
                })
                .function("notExists", returns(Type.Z).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Files.notExists(path));
                })
                .function("isDirectory", returns(Type.Z).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Files.isDirectory(path));
                })
                .function("isRegularFile", returns(Type.Z).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Files.isRegularFile(path));
                })
                .function("isSymbolicLink", returns(Type.Z).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(Files.isSymbolicLink(path));
                })
                .function("walk", returns(Type.OBJECT).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    try (Stream<Path> stream = Files.walk(path)) {
                        context.setReturnRef(stream.collect(Collectors.toList()));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
                    }
                })
                .function("walk", returns(Type.OBJECT).params(Type.I), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    int maxDepth = Coerce.asInteger(context.getArgBoxed(0)).orElse(Integer.MAX_VALUE);
                    try (Stream<Path> stream = Files.walk(path, maxDepth)) {
                        context.setReturnRef(stream.collect(Collectors.toList()));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
                    }
                });
    }
}
