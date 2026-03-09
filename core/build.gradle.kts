dependencies {
    implementation("org.ow2.asm:asm:9.5")
    implementation("it.unimi.dsi:fastutil:8.5.9")
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-XDignore.symbol.file")
}

tasks.register<JavaExec>("dumpFluxonCatalog") {
    group = "fluxon"
    description = "Generates the Fluxon function catalog JSON."
    mainClass.set("org.tabooproject.fluxon.tool.FunctionDumper")
    classpath = sourceSets["main"].runtimeClasspath
    args(layout.buildDirectory.file("fluxon-functions.json").get().asFile.absolutePath)
}

tasks.register<JavaExec>("benchmark") {
    group = "fluxon"
    description = "Run interpreter benchmark"
    mainClass.set("org.tabooproject.fluxon.interpreter.customized.ComplexTest")
    classpath = sourceSets["test"].runtimeClasspath
    maxHeapSize = "8g"
    jvmArgs("-XX:+UseSerialGC", "-Xmn7g")
}

tasks.jar {
    archiveBaseName = "fluxon-core"
}
repositories {
    mavenCentral()
}