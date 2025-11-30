pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.0"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "fluxon"
include(":core")
include(":core-console")
include(":core-jsr223")
include(":core-web-backend")
