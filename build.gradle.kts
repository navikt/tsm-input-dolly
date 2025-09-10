plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.flyway)
}

group = "no.nav.tsm"
version = "0.0.1"

val koinVersion = "4.1.0-Beta8"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
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
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.tsm.sykmelding.input)
    implementation(libs.logback.encoder)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation("io.insert-koin:koin-ktor3:${koinVersion}")
    implementation("io.insert-koin:koin-logger-slf4j:${koinVersion}")
}
