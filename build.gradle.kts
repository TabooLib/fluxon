plugins {
    java
    id("application")
    id("me.champeau.jmh") version "0.7.2"
    `maven-publish`
    jacoco
}

repositories {
    mavenCentral()
}

subprojects {
    apply<JavaPlugin>()
    apply(plugin = "application")
    apply(plugin = "me.champeau.jmh")
    apply(plugin = "maven-publish")
    apply(plugin = "jacoco")

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
        // org.jetbrains.annotations
        implementation("org.jetbrains:annotations:23.0.0")

        // JEXL 表达式引擎 - 用于性能对比测试
        testImplementation("org.apache.commons:commons-jexl3:3.3")
    }

    tasks.test {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.isDeprecation = false
        options.isWarnings = false
    }

//    // benchmark 模块使用 Java 17，其他模块使用 Java 8
//    if (project.name != "core-benchmark") {
//        configure<JavaPluginConvention> {
//            sourceCompatibility = JavaVersion.VERSION_1_8
//            targetCompatibility = JavaVersion.VERSION_1_8
//        }
//        java {
//            toolchain {
//                languageVersion.set(JavaLanguageVersion.of(8))
//            }
//        }
//    }

    jmh {
        iterations.set(5)
        warmupIterations.set(3)
        fork.set(1)
        includeTests.set(true)
    }

    // Maven 发布配置
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                groupId = "org.tabooproject.fluxon"
                artifactId = project.name
                version = rootProject.version.toString()
            }
        }
        repositories {
            maven {
                url = uri("https://repo.tabooproject.org/repository/releases")
                credentials {
                    username = project.findProperty("taboolibUsername")?.toString() ?: ""
                    password = project.findProperty("taboolibPassword")?.toString() ?: ""
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }
}