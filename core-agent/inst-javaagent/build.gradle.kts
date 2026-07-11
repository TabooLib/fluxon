tasks.jar {
    archiveBaseName = "fluxon-inst-javaagent"
    // 包含所有依赖，构建 fat jar
    // 此模块实际只依赖 ASM，避免 Java Agent 向系统类加载器暴露 Guava、JLine 等无关依赖
    from({
        configurations.runtimeClasspath.get()
            .filter { it.exists() }
            .filter { it.name.startsWith("asm-") }
            .map { if (it.isDirectory) it else zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Premain-Class" to "org.tabooproject.fluxon.inst.agent.FluxonAgent",
            "Agent-Class" to "org.tabooproject.fluxon.inst.agent.FluxonAgent",
            "Can-Retransform-Classes" to "true",
            "Can-Redefine-Classes" to "true"
        )
    }
}
