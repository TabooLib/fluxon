package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

public class FunctionFile {

    public static void init(FluxonRuntime runtime) {
        // 创建 Path 对象
        runtime.registerFunction("fs:io", "path", Arrays.asList(1, 2), (context) -> {
            if (context.getArgumentCount() == 1) {
                return Paths.get(Coerce.asString(context.getArgument(0)).orElse(""));
            } else {
                return Paths.get(Objects.requireNonNull(context.getString(0)), Objects.requireNonNull(context.getString(1)));
            }
        });
        // 创建 File 对象
        runtime.registerFunction("fs:io", "file", Arrays.asList(1, 2), (context) -> {
            if (context.getArgumentCount() == 1) {
                return new File(Objects.requireNonNull(context.getString(0)));
            } else {
                Object parent = Objects.requireNonNull(context.getArgument(0));
                String child = Objects.requireNonNull(context.getString(1));
                if (parent instanceof File) {
                    return new File((File) parent, child);
                } else {
                    return new File(parent.toString(), child);
                }
            }
        });
    }
}

