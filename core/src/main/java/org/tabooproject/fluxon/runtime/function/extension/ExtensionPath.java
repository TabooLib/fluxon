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
                .function("name", returns(Type.STRING).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    Path fileName = path.getFileName();
                    context.setReturnRef(fileName != null ? fileName.toString() : "");
                })
                .function("parent", returns(Type.PATH).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.getParent());
                })
                .function("root", returns(Type.PATH).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.getRoot());
                })
                .function("resolve", returns(Type.PATH).params(Type.STRING), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    String other = context.getString(0);
                    context.setReturnRef(path.resolve(other != null ? other : ""));
                })
                .function("relativize", returns(Type.PATH).params(Type.OBJECT), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    Object arg = context.getRef(0);
                    Path other = arg instanceof Path ? (Path) arg : Paths.get(arg.toString());
                    context.setReturnRef(path.relativize(other));
                })
                .function("normalize", returns(Type.PATH).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.normalize());
                })
                .function("toAbsolutePath", returns(Type.PATH).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.toAbsolutePath());
                })
                .function("toRealPath", returns(Type.PATH).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    try {
                        context.setReturnRef(path.toRealPath());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to get real path: " + e.getMessage(), e);
                    }
                })
                .function("toFile", returns(Type.FILE).noParams(), context -> {
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
                .function("walk", returns(Type.LIST).noParams(), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    try (Stream<Path> stream = Files.walk(path)) {
                        context.setReturnRef(stream.collect(Collectors.toList()));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
                    }
                })
                .function("walk", returns(Type.LIST).params(Type.I), context -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    try (Stream<Path> stream = Files.walk(path, context.getInt(0))) {
                        context.setReturnRef(stream.collect(Collectors.toList()));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
                    }
                });
    }
}
