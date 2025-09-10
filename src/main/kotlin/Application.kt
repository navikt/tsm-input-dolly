package no.nav.tsm

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import no.nav.tsm.plugins.configureKoin

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureKoin()
    configureHealthChecks()
    configureAdministration()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureRouting()
}
