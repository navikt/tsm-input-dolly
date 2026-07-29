plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.flyway)
}

group = "no.nav.tsm"
version = "0.0.1"

application {
    mainClass = "no.nav.tsm.ApplicationKt"
}

dependencies {
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.serialization.jackson)
    implementation(ktorLibs.server.metrics.micrometer)
    implementation(ktorLibs.server.openapi)
    implementation(ktorLibs.server.swagger)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.callId)
    implementation(ktorLibs.server.di)
    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.apache5)
    implementation(ktorLibs.client.contentNegotiation)

    implementation(libs.logback.classic)
    implementation(libs.logback.encoder)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.khealth)

    implementation(libs.postgres)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    implementation(libs.tsm.sykmelding.input)
    implementation(tsmKtorLibs.core)

    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.hamcrest)
    testImplementation(libs.rest.assured)
    testImplementation(libs.swagger.request.validator.rest.assured)
}
tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles {}
        from("src/main/resources/logback.xml") {
            into("/")
        }
    }
    test {
        useJUnitPlatform()
    }
}
