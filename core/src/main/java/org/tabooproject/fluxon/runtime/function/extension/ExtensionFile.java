package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtensionFile {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, ExtensionFile.class);
    }

    @FluxonFunction(value = "name", target = File.class, namespace = "fs:io")
    public static String name(File file) {
        return Objects.requireNonNull(file).getName();
    }

    @FluxonFunction(value = "path", target = File.class, namespace = "fs:io")
    public static String path(File file) {
        return Objects.requireNonNull(file).getPath();
    }

    @FluxonFunction(value = "absolutePath", target = File.class, namespace = "fs:io")
    public static String absolutePath(File file) {
        return Objects.requireNonNull(file).getAbsolutePath();
    }

    @FluxonFunction(value = "canonicalPath", target = File.class, namespace = "fs:io")
    public static String canonicalPath(File file) {
        try {
            return Objects.requireNonNull(file).getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get canonical path: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "parent", target = File.class, namespace = "fs:io")
    public static String parent(File file) {
        return Objects.requireNonNull(file).getParent();
    }

    @FluxonFunction(value = "parentFile", target = File.class, namespace = "fs:io")
    public static File parentFile(File file) {
        return Objects.requireNonNull(file).getParentFile();
    }

    @FluxonFunction(value = "toPath", target = File.class, namespace = "fs:io")
    public static Path toPath(File file) {
        return Objects.requireNonNull(file).toPath();
    }

    @FluxonFunction(value = "exists", target = File.class, namespace = "fs:io")
    public static boolean exists(File file) {
        return Objects.requireNonNull(file).exists();
    }

    @FluxonFunction(value = "isDirectory", target = File.class, namespace = "fs:io")
    public static boolean isDirectory(File file) {
        return Objects.requireNonNull(file).isDirectory();
    }

    @FluxonFunction(value = "isFile", target = File.class, namespace = "fs:io")
    public static boolean isFile(File file) {
        return Objects.requireNonNull(file).isFile();
    }

    @FluxonFunction(value = "length", target = File.class, namespace = "fs:io")
    public static long length(File file) {
        return Objects.requireNonNull(file).length();
    }

    @FluxonFunction(value = "lastModified", target = File.class, namespace = "fs:io")
    public static long lastModified(File file) {
        return Objects.requireNonNull(file).lastModified();
    }

    @FluxonFunction(value = "list", target = File.class, namespace = "fs:io")
    public static Object list(File file) {
        String[] names = Objects.requireNonNull(file).list();
        return names != null ? Arrays.asList(names) : Collections.emptyList();
    }

    @FluxonFunction(value = "listFiles", target = File.class, namespace = "fs:io")
    public static Object listFiles(File file) {
        File[] files = Objects.requireNonNull(file).listFiles();
        return files != null ? Arrays.asList(files) : Collections.emptyList();
    }

    @FluxonFunction(value = "mkdir", target = File.class, namespace = "fs:io")
    public static boolean mkdir(File file) {
        return Objects.requireNonNull(file).mkdir();
    }

    @FluxonFunction(value = "mkdirs", target = File.class, namespace = "fs:io")
    public static boolean mkdirs(File file) {
        return Objects.requireNonNull(file).mkdirs();
    }

    @FluxonFunction(value = "createNewFile", target = File.class, namespace = "fs:io")
    public static boolean createNewFile(File file) {
        try {
            return Objects.requireNonNull(file).createNewFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "delete", target = File.class, namespace = "fs:io")
    public static boolean delete(File file) {
        return Objects.requireNonNull(file).delete();
    }

    @FluxonFunction(value = "deleteOnExit", target = File.class, namespace = "fs:io")
    public static File deleteOnExit(File file) {
        Objects.requireNonNull(file).deleteOnExit();
        return file;
    }

    @FluxonFunction(value = "renameTo", target = File.class, namespace = "fs:io")
    public static boolean renameTo(File file, Object arg) {
        File to = arg instanceof File ? (File) arg : new File(arg.toString());
        return Objects.requireNonNull(file).renameTo(to);
    }

    @FluxonFunction(value = "readText", target = File.class, namespace = "fs:io")
    public static String readText(File file) {
        try {
            return new String(Files.readAllBytes(Objects.requireNonNull(file).toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "readLines", target = File.class, namespace = "fs:io")
    public static Object readLines(File file) {
        try {
            return Files.readAllLines(Objects.requireNonNull(file).toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "readBytes", target = File.class, namespace = "fs:io")
    public static Object readBytes(File file) {
        try {
            return Files.readAllBytes(Objects.requireNonNull(file).toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "writeText", target = File.class, namespace = "fs:io")
    public static File writeText(File file, String content) {
        Objects.requireNonNull(file);
        try {
            Files.write(file.toPath(), (content != null ? content : "").getBytes(StandardCharsets.UTF_8));
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "writeLines", target = File.class, namespace = "fs:io")
    @SuppressWarnings("unchecked")
    public static File writeLines(File file, Object arg) {
        Objects.requireNonNull(file);
        List<String> lines;
        if (arg instanceof List) {
            lines = ((List<?>) arg).stream().map(Object::toString).collect(Collectors.toList());
        } else if (arg != null) {
            lines = Collections.singletonList(arg.toString());
        } else {
            lines = Collections.emptyList();
        }
        try {
            Files.write(file.toPath(), lines);
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "writeBytes", target = File.class, namespace = "fs:io")
    public static File writeBytes(File file, Object arg) {
        Objects.requireNonNull(file);
        byte[] bytes;
        if (arg instanceof byte[]) {
            bytes = (byte[]) arg;
        } else if (arg != null) {
            bytes = arg.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            bytes = new byte[0];
        }
        try {
            Files.write(file.toPath(), bytes);
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "appendText", target = File.class, namespace = "fs:io")
    public static File appendText(File file, String content) {
        Objects.requireNonNull(file);
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(content != null ? content : "");
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Failed to append to file: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "copyTo", target = File.class, namespace = "fs:io")
    public static File copyTo1(File file, Object targetArg) {
        Objects.requireNonNull(file);
        File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
        try {
            Files.copy(file.toPath(), target.toPath());
            return target;
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy file: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "copyTo", target = File.class, namespace = "fs:io")
    public static File copyTo2(File file, Object targetArg, boolean replaceExisting) {
        Objects.requireNonNull(file);
        File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
        try {
            if (replaceExisting) {
                Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(file.toPath(), target.toPath());
            }
            return target;
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy file: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "moveTo", target = File.class, namespace = "fs:io")
    public static File moveTo1(File file, Object targetArg) {
        Objects.requireNonNull(file);
        File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
        try {
            Files.move(file.toPath(), target.toPath());
            return target;
        } catch (IOException e) {
            throw new RuntimeException("Failed to move file: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "moveTo", target = File.class, namespace = "fs:io")
    public static File moveTo2(File file, Object targetArg, boolean replaceExisting) {
        Objects.requireNonNull(file);
        File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
        try {
            if (replaceExisting) {
                Files.move(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(file.toPath(), target.toPath());
            }
            return target;
        } catch (IOException e) {
            throw new RuntimeException("Failed to move file: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "deleteRecursively", target = File.class, namespace = "fs:io")
    public static boolean deleteRecursivelyExt(File file) {
        return deleteRecursively(Objects.requireNonNull(file));
    }

    @FluxonFunction(value = "copyRecursively", target = File.class, namespace = "fs:io")
    public static File copyRecursively1(File file, Object targetArg) {
        Objects.requireNonNull(file);
        File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
        try {
            copyRecursively(file.toPath(), target.toPath(), false);
            return target;
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy recursively: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "copyRecursively", target = File.class, namespace = "fs:io")
    public static File copyRecursively2(File file, Object targetArg, boolean replaceExisting) {
        Objects.requireNonNull(file);
        File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
        try {
            copyRecursively(file.toPath(), target.toPath(), replaceExisting);
            return target;
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy recursively: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "extension", target = File.class, namespace = "fs:io")
    public static String extension(File file) {
        String name = Objects.requireNonNull(file).getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1) : "";
    }

    @FluxonFunction(value = "nameWithoutExtension", target = File.class, namespace = "fs:io")
    public static String nameWithoutExtension(File file) {
        String name = Objects.requireNonNull(file).getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : name;
    }

    @FluxonFunction(value = "walk", target = File.class, namespace = "fs:io")
    public static Object walk0(File file) {
        try (Stream<Path> stream = Files.walk(Objects.requireNonNull(file).toPath())) {
            return stream.map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
        }
    }

    @FluxonFunction(value = "walk", target = File.class, namespace = "fs:io")
    public static Object walk1(File file, int maxDepth) {
        try (Stream<Path> stream = Files.walk(Objects.requireNonNull(file).toPath(), maxDepth)) {
            return stream.map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
        }
    }

    private static boolean deleteRecursively(File file) {
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        return file.delete();
    }

    private static void copyRecursively(Path source, Path target, boolean replaceExisting) throws IOException {
        if (Files.isDirectory(source)) {
            if (!Files.exists(target)) {
                Files.createDirectories(target);
            }
            try (Stream<Path> stream = Files.list(source)) {
                for (Path child : stream.collect(Collectors.toList())) {
                    copyRecursively(child, target.resolve(source.relativize(child)), replaceExisting);
                }
            }
        } else {
            if (replaceExisting) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(source, target);
            }
        }
    }
}
