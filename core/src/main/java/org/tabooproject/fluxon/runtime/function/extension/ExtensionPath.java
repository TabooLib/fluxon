package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtensionPath {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionPath.class);
    }

    @FluxonFunction(value = "name", target = Path.class, namespace = "fs:io")
    public static String name(Path path) {
        Path fileName = Objects.requireNonNull(path).getFileName();
        return fileName != null ? fileName.toString() : "";
    }

    @FluxonFunction(value = "parent", target = Path.class, namespace = "fs:io")
    public static Object parent(Path path) {
        return Objects.requireNonNull(path).getParent();
    }

    @FluxonFunction(value = "root", target = Path.class, namespace = "fs:io")
    public static Object root(Path path) {
        return Objects.requireNonNull(path).getRoot();
    }

    @FluxonFunction(value = "resolve", target = Path.class, namespace = "fs:io")
    public static Object resolve(Path path, String other) {
        return Objects.requireNonNull(path).resolve(other != null ? other : "");
    }

    @FluxonFunction(value = "relativize", target = Path.class, namespace = "fs:io")
    public static Object relativize(Path path, Object arg) {
        Objects.requireNonNull(path);
        Path other = arg instanceof Path ? (Path) arg : Paths.get(arg.toString());
        return path.relativize(other);
    }

    @FluxonFunction(value = "normalize", target = Path.class, namespace = "fs:io")
    public static Object normalize(Path path) {
        return Objects.requireNonNull(path).normalize();
    }

    @FluxonFunction(value = "toAbsolutePath", target = Path.class, namespace = "fs:io")
    public static Object toAbsolutePath(Path path) {
        return Objects.requireNonNull(path).toAbsolutePath();
    }

    @FluxonFunction(value = "toRealPath", target = Path.class, namespace = "fs:io")
    public static Object toRealPath(Path path) {
        try {
            return Objects.requireNonNull(path).toRealPath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get real path: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "toFile", target = Path.class, namespace = "fs:io")
    public static File toFile(Path path) {
        return Objects.requireNonNull(path).toFile();
    }

    @FluxonFunction(value = "exists", target = Path.class, namespace = "fs:io")
    public static boolean exists(Path path) {
        return Files.exists(Objects.requireNonNull(path));
    }

    @FluxonFunction(value = "notExists", target = Path.class, namespace = "fs:io")
    public static boolean notExists(Path path) {
        return Files.notExists(Objects.requireNonNull(path));
    }

    @FluxonFunction(value = "isDirectory", target = Path.class, namespace = "fs:io")
    public static boolean isDirectory(Path path) {
        return Files.isDirectory(Objects.requireNonNull(path));
    }

    @FluxonFunction(value = "isRegularFile", target = Path.class, namespace = "fs:io")
    public static boolean isRegularFile(Path path) {
        return Files.isRegularFile(Objects.requireNonNull(path));
    }

    @FluxonFunction(value = "isSymbolicLink", target = Path.class, namespace = "fs:io")
    public static boolean isSymbolicLink(Path path) {
        return Files.isSymbolicLink(Objects.requireNonNull(path));
    }

    @FluxonFunction(value = "walk", target = Path.class, namespace = "fs:io")
    public static List<Path> walk0(Path path) {
        try (Stream<Path> stream = Files.walk(Objects.requireNonNull(path))) {
            return stream.collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "walk", target = Path.class, namespace = "fs:io")
    public static List<Path> walk1(Path path, int maxDepth) {
        try (Stream<Path> stream = Files.walk(Objects.requireNonNull(path), maxDepth)) {
            return stream.collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
        }
    }
}
