application {
    mainClass.set("org.tabooproject.fluxon.web.WebBackendServer")
}

dependencies {
    implementation(project(":core"))
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks.jar {
    archiveBaseName = "fluxon-core-web-backend"
    manifest {
        attributes["Main-Class"] = "org.tabooproject.fluxon.web.WebBackendServer"
    }
}

tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Assembles an uber-jar with all dependencies for the web backend."
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "org.tabooproject.fluxon.web.WebBackendServer"
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
