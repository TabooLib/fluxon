package org.tabooproject.fluxon.runtime.function.extension;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtensionPath {

    public static void init(FluxonRuntime runtime) {
        runtime.registerExtension(Path.class, "fs:io")
                // 获取文件名
                .function("name", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    Path fileName = path.getFileName();
                    context.setReturnRef(fileName != null ? fileName.toString() : "");
                })
                // 获取父路径
                .function("parent", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.getParent());
                })
                // 获取根路径
                .function("root", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.getRoot());
                })
                // 解析子路径
                .function("resolve", 1, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    String other = Coerce.asString(context.getRef(0)).orElse("");
                    context.setReturnRef(path.resolve(other));
                })
                // 相对化路径
                .function("relativize", 1, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    Object arg = context.getRef(0);
                    Path other = arg instanceof Path ? (Path) arg : Paths.get(arg.toString());
                    context.setReturnRef(path.relativize(other));
                })
                // 规范化路径
                .function("normalize", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.normalize());
                })
                // 转换为绝对路径
                .function("toAbsolutePath", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.toAbsolutePath());
                })
                // 转换为真实路径
                .function("toRealPath", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    try {
                        context.setReturnRef(path.toRealPath());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to get real path: " + e.getMessage(), e);
                    }
                })
                // 转换为文件
                .function("toFile", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(path.toFile());
                })
                // 检查是否存在
                .function("exists", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Files.exists(path));
                })
                // 检查是否不存在
                .function("notExists", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Files.notExists(path));
                })
                // 检查是否为目录
                .function("isDirectory", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Files.isDirectory(path));
                })
                // 检查是否为常规文件
                .function("isRegularFile", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Files.isRegularFile(path));
                })
                // 检查是否为符号链接
                .function("isSymbolicLink", 0, (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    context.setReturnRef(Files.isSymbolicLink(path));
                })
                // 递归遍历目录树
                .function("walk", Arrays.asList(0, 1), (context) -> {
                    Path path = Objects.requireNonNull(context.getTarget());
                    int maxDepth = 0 < context.getArgumentCount() ? Coerce.asInteger(context.getArgBoxed(0)).orElse(Integer.MAX_VALUE) : Integer.MAX_VALUE;
                    try (Stream<Path> stream = Files.walk(path, maxDepth)) {
                        context.setReturnRef(stream.collect(Collectors.toList()));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to walk directory tree: " + e.getMessage(), e);
                    }
                });
    }
}
