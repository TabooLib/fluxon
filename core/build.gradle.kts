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

tasks.register<JavaExec>("benchmarkEnv") {
    group = "fluxon"
    description = "Run Environment benchmark"
    mainClass.set("org.tabooproject.fluxon.benchmark.EnvironmentReuseBenchmark")
    classpath = sourceSets["test"].runtimeClasspath
}

tasks.register<Exec>("decompileTestFs") {
    group = "fluxon"
    description = "Decompile src/test/fs class artifacts with Vineflower."
    val vineflowerJar = providers.gradleProperty("vineflowerJar")
    val classFilter = providers.gradleProperty("fluxonTestFsClasses")
    val sourceDir = layout.projectDirectory.dir("src/test/fs")
    val outputDir = layout.buildDirectory.dir("decompiled-test-fs")
    inputs.files(fileTree(sourceDir) { include("*.class") })
    outputs.dir(outputDir)
    doFirst {
        val jarFile = file(vineflowerJar.get())
        if (!jarFile.isFile) {
            throw IllegalStateException("Vineflower jar not found: ${jarFile.absolutePath}")
        }
        val selectedNames = if (classFilter.isPresent) {
            classFilter.get().split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        } else {
            emptySet()
        }
        val classFiles = fileTree(sourceDir) {
            include("*.class")
        }.files.filter { selectedNames.isEmpty() || selectedNames.contains(it.nameWithoutExtension) }.sortedBy { it.name }
        if (classFiles.isEmpty()) {
            throw IllegalStateException("No test fs class files matched.")
        }
        val out = outputDir.get().asFile
        out.mkdirs()
        commandLine(listOf("java", "-jar", jarFile.absolutePath, "--folder", "--silent") + classFiles.map { it.absolutePath } + out.absolutePath)
    }
}

tasks.jar {
    archiveBaseName = "fluxon-core"
}
repositories {
    mavenCentral()
}
