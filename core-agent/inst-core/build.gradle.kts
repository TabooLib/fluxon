dependencies {
    implementation(project(":core"))
    implementation("org.ow2.asm:asm:9.5")
    implementation("org.ow2.asm:asm-commons:9.5")
    
    // Mockito 4.x is the last version supporting Java 8
    testImplementation("org.mockito:mockito-core:4.11.0")
}

tasks.jar {
    archiveBaseName = "fluxon-inst-core"
}

// 端到端测试需要使用 javaagent
tasks.test {
    dependsOn(":core-agent:inst-javaagent:jar")
    doFirst {
        val agentJarDir = project(":core-agent:inst-javaagent").layout.buildDirectory.dir("libs").get().asFile
        val agentJar = agentJarDir.listFiles()?.find { it.name.startsWith("fluxon-inst-javaagent") && it.name.endsWith(".jar") }
            ?: throw GradleException("Agent jar not found in $agentJarDir")
        jvmArgs("-javaagent:${agentJar.absolutePath}")
    }
}
