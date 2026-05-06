val agent: Configuration by configurations.creating

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
    id("nl.littlerobots.version-catalog-update") version "1.1.0"
}

group = "com.netbug94.heraldo"
version = "1.0.0-SNAPSHOT"

application {
    mainClass.set("io.ktor.server.cio.EngineMain")
    // IPv4 preference often needed for local networking/Docker
    applicationDefaultJvmArgs = listOf("-Djava.net.preferIPv4Stack=true")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // --- Ktor Server ---
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.cio)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.websockets)
    implementation(ktorLibs.serialization.kotlinx.json)

    // --- Ktor Client ---
    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.cio)
    implementation(ktorLibs.client.contentNegotiation)

    // --- DI & Logging ---
    implementation(libs.koin.ktor)
    implementation(libs.koin.loggerSlf4j)
    implementation(libs.logback.classic)

    // --- Google APIs ---
    implementation(libs.google.api.client)
    implementation(libs.google.oauth.jetty)
    implementation(libs.google.apis.tasks)
    implementation(libs.google.apis.calendar)

    // --- Utilities ---
    implementation(libs.kotlinx.coroutines.core)

    // --- Testing ---
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(ktorLibs.client.mock)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    // ByteBuddy agent for MockK static mocking
    agent(libs.bytebuddy.agent)
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Essential for MockK to work with final classes in JDK 21+
    jvmArgs("-javaagent:${agent.singleFile}")
}