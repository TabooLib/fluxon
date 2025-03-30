dependencies {
    implementation(project(":core"))
    implementation(project(":core-jsr223"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.tabooproject.fluxon.FluxonLine"
    }
}