plugins {
    kotlin("jvm")
}
dependencies {
    implementation("org.ow2.asm:asm:9.5")
    testImplementation(kotlin("stdlib-jdk8"))
}

tasks.jar {
    archiveBaseName = "fluxon-core"
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(8)
}