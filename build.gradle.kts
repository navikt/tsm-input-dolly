import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

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
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.jackson3)
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

    implementation(libs.postgres)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    implementation(libs.tsm.sykmeldinger.input)
    implementation(tsmKtorLibs.core)
    implementation(tsmKtorLibs.kafka.sykmeldinger)

    testImplementation(tsmKtorLibs.kafka.test)
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
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

    named<DependencyUpdatesTask>("dependencyUpdates") {
        fun String.isNonStable(): Boolean {
            val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
            val regex = "^[0-9,.v-]+(-r)?$".toRegex()
            val isStable = stableKeyword || regex.matches(this)
            return isStable.not()
        }

        rejectVersionIf {
            candidate.version.isNonStable()
        }
    }
}
