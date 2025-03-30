dependencies {
    implementation(project(":core"))
}

tasks.jar {
    archiveBaseName = "fluxon-core-jsr223"
}