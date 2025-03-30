dependencies {
    implementation(project(":core"))
    implementation(project(":core-jsr223"))
}

tasks.jar {
    archiveBaseName = "fluxon-core-console"
    manifest {
        attributes["Main-Class"] = "org.tabooproject.fluxon.FluxonLine"
    }
}