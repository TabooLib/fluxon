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
        runtime.registerFunction("fs:io", "path", Arrays.asList(1, 2), context -> {
            if (context.getArgumentCount() == 1) {
                context.setReturnRef(Paths.get(Coerce.asString(context.getRef(0)).orElse("")));
            } else {
                context.setReturnRef(Paths.get(
                        Objects.requireNonNull(Objects.toString(context.getRef(0), null)),
                        Objects.requireNonNull(Objects.toString(context.getRef(1), null))
                ));
            }
        });
        // 创建 File 对象
        runtime.registerFunction("fs:io", "file", Arrays.asList(1, 2), context -> {
            if (context.getArgumentCount() == 1) {
                context.setReturnRef(new File(Objects.requireNonNull(Objects.toString(context.getRef(0), null))));
            } else {
                Object parent = Objects.requireNonNull(context.getRef(0));
                String child = Objects.requireNonNull(Objects.toString(context.getRef(1), null));
                if (parent instanceof File) {
                    context.setReturnRef(new File((File) parent, child));
                } else {
                    context.setReturnRef(new File(parent.toString(), child));
                }
            }
        });
    }
}
