package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class ExtensionFile {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(File.class, "fs:io")
                .function("name", returns(Type.STRING).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(file.getName());
                })
                .function("path", returns(Type.STRING).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(file.getPath());
                })
                .function("absolutePath", returns(Type.STRING).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(file.getAbsolutePath());
                })
                .function("canonicalPath", returns(Type.STRING).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    try {
                        context.setReturnRef(file.getCanonicalPath());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to get canonical path: " + e.getMessage(), e);
                    }
                })
                .function("parent", returns(Type.STRING).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(file.getParent());
                })
                .function("parentFile", returns(Type.FILE).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(file.getParentFile());
                })
                .function("toPath", returns(Type.PATH).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(file.toPath());
                })
                .function("exists", returns(Type.Z).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(file.exists());
                })
                .function("isDirectory", returns(Type.Z).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(file.isDirectory());
                })
                .function("isFile", returns(Type.Z).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(file.isFile());
                })
                .function("length", returns(Type.J).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnLong(file.length());
                })
                .function("lastModified", returns(Type.J).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnLong(file.lastModified());
                })
                .function("list", returns(Type.OBJECT).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    String[] names = file.list();
                    context.setReturnRef(names != null ? java.util.Arrays.asList(names) : Collections.emptyList());
                })
                .function("listFiles", returns(Type.OBJECT).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    File[] files = file.listFiles();
                    context.setReturnRef(files != null ? java.util.Arrays.asList(files) : Collections.emptyList());
                })
                .function("mkdir", returns(Type.Z).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(file.mkdir());
                })
                .function("mkdirs", returns(Type.Z).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(file.mkdirs());
                })
                .function("createNewFile", returns(Type.Z).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    try {
                        context.setReturnBool(file.createNewFile());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to create file: " + e.getMessage(), e);
                    }
                })
                .function("delete", returns(Type.Z).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(file.delete());
                })
                .function("deleteOnExit", returns(Type.FILE).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    file.deleteOnExit();
                    context.setReturnRef(file);
                })
                .function("renameTo", returns(Type.Z).params(Type.OBJECT), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object arg = context.getRef(0);
                    File to = arg instanceof File ? (File) arg : new File(arg.toString());
                    context.setReturnBool(file.renameTo(to));
                })
                .function("readText", returns(Type.STRING).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    try {
                        context.setReturnRef(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
                    }
                })
                .function("readLines", returns(Type.OBJECT).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    try {
                        context.setReturnRef(Files.readAllLines(file.toPath()));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
                    }
                })
                .function("readBytes", returns(Type.OBJECT).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    try {
                        context.setReturnRef(Files.readAllBytes(file.toPath()));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
                    }
                })
                .function("writeText", returns(Type.FILE).params(Type.STRING), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    String content = (String) context.getRef(0);
                    try {
                        Files.write(file.toPath(), (content != null ? content : "").getBytes(StandardCharsets.UTF_8));
                        context.setReturnRef(file);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to write file: " + e.getMessage(), e);
                    }
                })
                .function("writeLines", returns(Type.FILE).params(Type.OBJECT), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object arg = context.getRef(0);
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
                        context.setReturnRef(file);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to write file: " + e.getMessage(), e);
                    }
                })
                .function("writeBytes", returns(Type.FILE).params(Type.OBJECT), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object arg = context.getRef(0);
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
                        context.setReturnRef(file);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to write file: " + e.getMessage(), e);
                    }
                })
                .function("appendText", returns(Type.FILE).params(Type.STRING), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    String content = (String) context.getRef(0);
                    try (FileWriter writer = new FileWriter(file, true)) {
                        writer.write(content != null ? content : "");
                        context.setReturnRef(file);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to append to file: " + e.getMessage(), e);
                    }
                })
                .function("copyTo", returns(Type.FILE).params(Type.OBJECT), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object targetArg = context.getRef(0);
                    File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
                    try {
                        Files.copy(file.toPath(), target.toPath());
                        context.setReturnRef(target);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy file: " + e.getMessage(), e);
                    }
                })
                .function("copyTo", returns(Type.FILE).params(Type.OBJECT, Type.Z), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object targetArg = context.getRef(0);
                    File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
                    boolean replaceExisting = Coerce.asBoolean(context.getArgBoxed(1)).orElse(false);
                    try {
                        if (replaceExisting) {
                            Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.copy(file.toPath(), target.toPath());
                        }
                        context.setReturnRef(target);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy file: " + e.getMessage(), e);
                    }
                })
                .function("moveTo", returns(Type.FILE).params(Type.OBJECT), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object targetArg = context.getRef(0);
                    File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
                    try {
                        Files.move(file.toPath(), target.toPath());
                        context.setReturnRef(target);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to move file: " + e.getMessage(), e);
                    }
                })
                .function("moveTo", returns(Type.FILE).params(Type.OBJECT, Type.Z), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object targetArg = context.getRef(0);
                    File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
                    boolean replaceExisting = Coerce.asBoolean(context.getArgBoxed(1)).orElse(false);
                    try {
                        if (replaceExisting) {
                            Files.move(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.move(file.toPath(), target.toPath());
                        }
                        context.setReturnRef(target);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to move file: " + e.getMessage(), e);
                    }
                })
                .function("deleteRecursively", returns(Type.Z).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    context.setReturnBool(deleteRecursively(file));
                })
                .function("copyRecursively", returns(Type.FILE).params(Type.OBJECT), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object targetArg = context.getRef(0);
                    File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
                    try {
                        copyRecursively(file.toPath(), target.toPath(), false);
                        context.setReturnRef(target);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy recursively: " + e.getMessage(), e);
                    }
                })
                .function("copyRecursively", returns(Type.FILE).params(Type.OBJECT, Type.Z), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object targetArg = context.getRef(0);
                    File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
                    boolean replaceExisting = Coerce.asBoolean(context.getArgBoxed(1)).orElse(false);
                    try {
                        copyRecursively(file.toPath(), target.toPath(), replaceExisting);
                        context.setReturnRef(target);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy recursively: " + e.getMessage(), e);
                    }
                })
                .function("extension", returns(Type.STRING).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    String name = file.getName();
                    int lastDot = name.lastIndexOf('.');
                    context.setReturnRef(lastDot > 0 ? name.substring(lastDot + 1) : "");
                })
                .function("nameWithoutExtension", returns(Type.STRING).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    String name = file.getName();
                    int lastDot = name.lastIndexOf('.');
                    context.setReturnRef(lastDot > 0 ? name.substring(0, lastDot) : name);
                })
                .function("walk", returns(Type.OBJECT).noParams(), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    try (Stream<Path> stream = Files.walk(file.toPath())) {
                        context.setReturnRef(stream.map(Path::toFile).collect(Collectors.toList()));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
                    }
                })
                .function("walk", returns(Type.OBJECT).params(Type.I), context -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    int maxDepth = Coerce.asInteger(context.getArgBoxed(0)).orElse(Integer.MAX_VALUE);
                    try (Stream<Path> stream = Files.walk(file.toPath(), maxDepth)) {
                        context.setReturnRef(stream.map(Path::toFile).collect(Collectors.toList()));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
                    }
                });
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
