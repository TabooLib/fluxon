package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtensionPath {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Path.class, "fs:io")
                // 获取文件名
                .function("name", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    Path fileName = path.getFileName();
                    return fileName != null ? fileName.toString() : "";
                })
                // 获取父路径
                .function("parent", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    return path.getParent();
                })
                // 获取根路径
                .function("root", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    return path.getRoot();
                })
                // 解析子路径
                .function("resolve", 1, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    String other = Coerce.asString(context.getArgument(0)).orElse("");
                    return path.resolve(other);
                })
                // 相对化路径
                .function("relativize", 1, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    Object arg = context.getArgument(0);
                    Path other = arg instanceof Path ? (Path) arg : Paths.get(arg.toString());
                    return path.relativize(other);
                })
                // 规范化路径
                .function("normalize", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    return path.normalize();
                })
                // 转换为绝对路径
                .function("toAbsolutePath", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    return path.toAbsolutePath();
                })
                // 转换为真实路径
                .function("toRealPath", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    try {
                        return path.toRealPath();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to get real path: " + e.getMessage(), e);
                    }
                })
                // 转换为文件
                .function("toFile", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    return path.toFile();
                })
                // 检查是否存在
                .function("exists", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    return Files.exists(path);
                })
                // 检查是否不存在
                .function("notExists", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    return Files.notExists(path);
                })
                // 检查是否为目录
                .function("isDirectory", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    return Files.isDirectory(path);
                })
                // 检查是否为常规文件
                .function("isRegularFile", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    return Files.isRegularFile(path);
                })
                // 检查是否为符号链接
                .function("isSymbolicLink", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    return Files.isSymbolicLink(path);
                })
                // 递归遍历目录树
                .function("walk", Arrays.asList(0, 1), (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    int maxDepth = context.hasArgument(0) ? Coerce.asInteger(context.getArgument(0)).orElse(Integer.MAX_VALUE) : Integer.MAX_VALUE;
                    try (Stream<Path> stream = Files.walk(path, maxDepth)) {
                        return stream.collect(Collectors.toList());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
                    }
                });
    }
}

