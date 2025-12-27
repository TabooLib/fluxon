package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

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
        runtime.registerExtension(File.class, "fs:io")
                // 获取文件名
                .function("name", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.getName();
                })
                // 获取路径
                .function("path", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.getPath();
                })
                // 获取绝对路径
                .function("absolutePath", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.getAbsolutePath();
                })
                // 获取规范路径
                .function("canonicalPath", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    try {
                        return file.getCanonicalPath();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to get canonical path: " + e.getMessage(), e);
                    }
                })
                // 获取父目录
                .function("parent", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.getParent();
                })
                // 获取父目录文件对象
                .function("parentFile", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.getParentFile();
                })
                // 转换为 Path
                .function("toPath", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.toPath();
                })
                // 检查是否存在
                .function("exists", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.exists();
                })
                // 检查是否为目录
                .function("isDirectory", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.isDirectory();
                })
                // 检查是否为文件
                .function("isFile", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.isFile();
                })
                // 获取文件大小
                .function("length", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.length();
                })
                // 获取最后修改时间
                .function("lastModified", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.lastModified();
                })
                // 列出文件名
                .function("list", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    String[] names = file.list();
                    return names != null ? Arrays.asList(names) : Collections.emptyList();
                })
                // 列出文件对象
                .function("listFiles", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    File[] files = file.listFiles();
                    return files != null ? Arrays.asList(files) : Collections.emptyList();
                })
                // 创建目录
                .function("mkdir", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.mkdir();
                })
                // 创建所有目录
                .function("mkdirs", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.mkdirs();
                })
                // 创建文件
                .function("createNewFile", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    try {
                        return file.createNewFile();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to create file: " + e.getMessage(), e);
                    }
                })
                // 删除文件
                .function("delete", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return file.delete();
                })
                // 退出时删除
                .function("deleteOnExit", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    file.deleteOnExit();
                    return file;
                })
                // 重命名
                .function("renameTo", 1, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object arg = context.getArgument(0);
                    File to = arg instanceof File ? (File) arg : new File(arg.toString());
                    return file.renameTo(to);
                })
                // 读取所有文本
                .function("readText", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    try {
                        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
                    }
                })
                // 读取所有行
                .function("readLines", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    try {
                        return Files.readAllLines(file.toPath());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
                    }
                })
                // 读取所有字节
                .function("readBytes", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    try {
                        return Files.readAllBytes(file.toPath());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
                    }
                })
                // 写入文本
                .function("writeText", 1, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    String content = Coerce.asString(context.getArgument(0)).orElse("");
                    try {
                        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
                        return file;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to write file: " + e.getMessage(), e);
                    }
                })
                // 写入行
                .function("writeLines", 1, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object arg = context.getArgument(0);
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
                })
                // 写入字节
                .function("writeBytes", 1, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object arg = context.getArgument(0);
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
                })
                // 追加文本
                .function("appendText", 1, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    String content = Coerce.asString(context.getArgument(0)).orElse("");
                    try (FileWriter writer = new FileWriter(file, true)) {
                        writer.write(content);
                        return file;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to append to file: " + e.getMessage(), e);
                    }
                })
                // 复制到
                .function("copyTo", Arrays.asList(1, 2), (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object targetArg = context.getArgument(0);
                    File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
                    try {
                        if (context.hasArgument(1)) {
                            boolean replaceExisting = Coerce.asBoolean(context.getArgument(1)).orElse(false);
                            if (replaceExisting) {
                                Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            } else {
                                Files.copy(file.toPath(), target.toPath());
                            }
                        } else {
                            Files.copy(file.toPath(), target.toPath());
                        }
                        return target;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy file: " + e.getMessage(), e);
                    }
                })
                // 移动到
                .function("moveTo", Arrays.asList(1, 2), (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object targetArg = context.getArgument(0);
                    File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
                    try {
                        if (context.hasArgument(1)) {
                            boolean replaceExisting = Coerce.asBoolean(context.getArgument(1)).orElse(false);
                            if (replaceExisting) {
                                Files.move(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            } else {
                                Files.move(file.toPath(), target.toPath());
                            }
                        } else {
                            Files.move(file.toPath(), target.toPath());
                        }
                        return target;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to move file: " + e.getMessage(), e);
                    }
                })
                // 递归删除目录
                .function("deleteRecursively", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    return deleteRecursively(file);
                })
                // 递归复制目录
                .function("copyRecursively", Arrays.asList(1, 2), (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    Object targetArg = context.getArgument(0);
                    File target = targetArg instanceof File ? (File) targetArg : new File(targetArg.toString());
                    boolean replaceExisting = context.hasArgument(1) ? Coerce.asBoolean(context.getArgument(1)).orElse(false) : false;
                    try {
                        copyRecursively(file.toPath(), target.toPath(), replaceExisting);
                        return target;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy recursively: " + e.getMessage(), e);
                    }
                })
                // 获取扩展名
                .function("extension", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    String name = file.getName();
                    int lastDot = name.lastIndexOf('.');
                    return lastDot > 0 ? name.substring(lastDot + 1) : "";
                })
                // 获取不带扩展名的文件名
                .function("nameWithoutExtension", 0, (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    String name = file.getName();
                    int lastDot = name.lastIndexOf('.');
                    return lastDot > 0 ? name.substring(0, lastDot) : name;
                })
                // 遍历目录树
                .function("walk", Arrays.asList(0, 1), (context) -> {
                    File file = Objects.requireNonNull(context.getTarget());
                    int maxDepth = context.hasArgument(0) ? Coerce.asInteger(context.getArgument(0)).orElse(Integer.MAX_VALUE) : Integer.MAX_VALUE;
                    try (Stream<Path> stream = Files.walk(file.toPath(), maxDepth)) {
                        return stream.map(Path::toFile).collect(Collectors.toList());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
                    }
                });
    }

    /**
     * 递归删除文件或目录
     */
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

    /**
     * 递归复制文件或目录
     */
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

