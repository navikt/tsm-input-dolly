package no.nav.tsm

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import no.nav.tsm.plugins.configureDependencies
import no.nav.tsm.plugins.configureDatabase
import no.nav.tsm.plugins.configureSwagger
import no.nav.tsm.plugins.configureMonitoring
import no.nav.tsm.plugins.configureSerialization
import no.nav.tsm.sykmelding.configureRouting
import no.nav.tsm.sykmelding.consumer.configureSykmeldingConsumer

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDependencies()
    configureDatabase()
    configureMonitoring()
    configureSerialization()
    configureSwagger()

    configureSykmeldingConsumer()
    configureRouting()
}
