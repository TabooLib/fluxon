package org.tabooproject.fluxon.runtime.function.domain;

import org.tabooproject.fluxon.parser.DomainRegistry;

public class DomainExtension {

    public static void init() {
        // with - 执行结束返回闭包最后一行
        DomainRegistry.primary().register("with", (env, body) -> body.get());
        // also - 执行结束返回 target
        DomainRegistry.primary().register("also", (env, body) -> {
            body.get();
            return env.getTarget();
        });
    }
}