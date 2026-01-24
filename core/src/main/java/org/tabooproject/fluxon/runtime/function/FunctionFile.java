package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class FunctionFile {

    public static void init(FluxonRuntime runtime) {
        // path(string)
        runtime.registerFunction("fs:io", "path", returns(Type.OBJECT).params(Type.OBJECT), context -> {
            context.setReturnRef(Paths.get(Coerce.asString(context.getRef(0)).orElse("")));
        });
        // path(string, string)
        runtime.registerFunction("fs:io", "path", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), context -> {
            context.setReturnRef(Paths.get(
                    Objects.requireNonNull(Objects.toString(context.getRef(0), null)),
                    Objects.requireNonNull(Objects.toString(context.getRef(1), null))
            ));
        });
        // file(string)
        runtime.registerFunction("fs:io", "file", returns(Type.OBJECT).params(Type.OBJECT), context -> {
            context.setReturnRef(new File(Objects.requireNonNull(Objects.toString(context.getRef(0), null))));
        });
        // file(parent, child)
        runtime.registerFunction("fs:io", "file", returns(Type.OBJECT).params(Type.OBJECT, Type.OBJECT), context -> {
            Object parent = Objects.requireNonNull(context.getRef(0));
            String child = Objects.requireNonNull(Objects.toString(context.getRef(1), null));
            if (parent instanceof File) {
                context.setReturnRef(new File((File) parent, child));
            } else {
                context.setReturnRef(new File(parent.toString(), child));
            }
        });
    }
}
