tasks.jar {
    archiveBaseName = "fluxon-inst-javaagent"
    // 包含所有依赖，构建 fat jar
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }
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
