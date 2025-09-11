plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.flyway)
}

group = "no.nav.tsm"
version = "0.0.1"


application {
    mainClass = "no.nav.tsm.ApplicationKt"
}

repositories {
    mavenCentral()
    maven { url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
}

dependencies {
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.metrics)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.callid)
    implementation(libs.logback.classic)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.tsm.sykmelding.input)
    implementation(libs.logback.encoder)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger)
    implementation(libs.postgres)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks {
    shadowJar {
        mergeServiceFiles {
        }
    }
}
