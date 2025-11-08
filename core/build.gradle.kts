dependencies {
    implementation("org.ow2.asm:asm:9.5")
    implementation("it.unimi.dsi:fastutil:8.5.9")
}

tasks.jar {
    archiveBaseName = "fluxon-core"
}
repositories {
    mavenCentral()
}