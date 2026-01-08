import org.gradle.api.tasks.JavaExec

application {
    mainClass.set("org.tabooproject.fluxon.FluxonConsole")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-jsr223"))
}

tasks.jar {
    archiveBaseName = "fluxon-core-console"
    manifest {
        attributes["Main-Class"] = "org.tabooproject.fluxon.FluxonConsole"
    }
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.register<JavaExec>("runSyntax") {
    description = "Run core-syntax.fs to verify syntax examples"
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.tabooproject.fluxon.FluxonConsole")
    args(rootProject.file("core-syntax.fs").absolutePath)
}