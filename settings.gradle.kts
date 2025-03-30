pluginManagement {
    plugins {
        kotlin("jvm") version "2.1.10"
    }
}
rootProject.name = "fluxon"
include(":core")
include(":core-fline")
include(":core-jsr223")