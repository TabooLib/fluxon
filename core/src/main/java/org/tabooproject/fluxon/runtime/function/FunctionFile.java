package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.Type;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

import static org.tabooproject.fluxon.runtime.FunctionSignature.returns;

public class FunctionFile {

    public static void init(FluxonRuntime runtime) {
        // path(string)
        runtime.registerFunction("fs:io", "path", returns(Type.PATH).params(Type.STRING), context -> {
            context.setReturnRef(Paths.get(context.getString(0)));
        });
        // path(string, string)
        runtime.registerFunction("fs:io", "path", returns(Type.PATH).params(Type.STRING, Type.STRING), context -> {
            context.setReturnRef(Paths.get(context.getString(0), context.getString(1)));
        });
        // file(string)
        runtime.registerFunction("fs:io", "file", returns(Type.FILE).params(Type.STRING), context -> {
            context.setReturnRef(new File(context.getString(0)));
        });
        // file(parent, child)
        runtime.registerFunction("fs:io", "file", returns(Type.FILE).params(Type.OBJECT, Type.STRING), context -> {
            Object parent = Objects.requireNonNull(context.getRef(0));
            String child = context.getString(1);
            if (parent instanceof File) {
                context.setReturnRef(new File((File) parent, child));
            } else {
                context.setReturnRef(new File(parent.toString(), child));
            }
        });
    }
}
