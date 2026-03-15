package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonFunction;
import org.tabooproject.fluxon.runtime.FluxonFunctionScanner;
import org.tabooproject.fluxon.runtime.FluxonRuntime;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * 文件与路径构造函数
 *
 * @author sky
 */
public class FunctionFile {

    public static void init(FluxonRuntime runtime) {
        FluxonFunctionScanner.register(runtime, FunctionFile.class);
    }

    // path(string) — 从字符串构造 Path
    @FluxonFunction(value = "path", namespace = "fs:io")
    public static Path path1(String s) {
        return Paths.get(s);
    }

    // path(string, string) — 从两个字符串构造 Path
    @FluxonFunction(value = "path", namespace = "fs:io")
    public static Path path2(String s1, String s2) {
        return Paths.get(s1, s2);
    }

    // file(string) — 从字符串构造 File
    @FluxonFunction(value = "file", namespace = "fs:io")
    public static File file1(String s) {
        return new File(s);
    }

    // file(parent, child) — 从父对象和子路径构造 File
    @FluxonFunction(value = "file", namespace = "fs:io")
    public static File file2(Object parent, String child) {
        Objects.requireNonNull(parent);
        if (parent instanceof File) {
            return new File((File) parent, child);
        }
        return new File(parent.toString(), child);
    }
}
