plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0" // 添加序列化插件
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.4.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.github.classgraph:classgraph:4.8.162")
    implementation("org.reflections:reflections:0.10.2")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("org.example.MainKt") // 注意：Main.kt 对应的 class 是 MainKt
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.example.MainKt"
    }

    // 把所有依赖一起打进 JAR（可选）
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}