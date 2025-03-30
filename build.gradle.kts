plugins {
    id("java")
    id("me.champeau.jmh") version "0.7.2"
    kotlin("jvm")
    id("application")
}

group = "org.tabooproject.fluxon"
version = "1.0-SNAPSHOT"

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
    
    implementation("org.ow2.asm:asm:9.4")
    implementation("org.ow2.asm:asm-commons:9.4")
    implementation(kotlin("stdlib-jdk8"))
}

// 设置应用程序主类
application {
    mainClass.set("org.tabooproject.fluxon.runtime.FluxonLine")
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
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// JMH 配置
jmh {
    iterations.set(5)
    warmupIterations.set(3)
    fork.set(1)
    includeTests.set(true)
}

// 创建可执行 jar
tasks.register<Jar>("fatJar") {
    archiveClassifier.set("fat")
    
    from(sourceSets.main.get().output)
    
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    
    manifest {
        attributes["Main-Class"] = "org.tabooproject.fluxon.fline.FLine"
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}