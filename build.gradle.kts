plugins {
    java
    id("application")
    id("me.champeau.jmh") version "0.7.2"
    kotlin("jvm") version "2.1.10"
}

repositories {
    mavenCentral()
}

subprojects {
    apply<JavaPlugin>()
    apply(plugin = "application")
    apply(plugin = "me.champeau.jmh")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        // JLine 相关依赖
        implementation("org.jline:jline:3.22.0")
        implementation("org.jline:jline-terminal-jansi:3.22.0")
        implementation("net.java.dev.jna:jna:5.12.1")

        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation("org.junit.jupiter:junit-jupiter-params")

        // JMH 性能测试框架
        testImplementation("org.openjdk.jmh:jmh-core:1.37")
        testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

        // Google Common
        implementation("com.google.guava:guava:31.0.1-jre")
        implementation("org.ow2.asm:asm:9.4")
        implementation("org.ow2.asm:asm-commons:9.4")
        implementation(kotlin("stdlib"))

        // JEXL 表达式引擎 - 用于性能对比测试
        testImplementation("org.apache.commons:commons-jexl3:3.3")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:unchecked")
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    jmh {
        iterations.set(5)
        warmupIterations.set(3)
        fork.set(1)
        includeTests.set(true)
    }
}