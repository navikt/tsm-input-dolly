package no.nav.tsm

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.tsm.sykmelding.SykmeldingService
import no.nav.tsm.sykmelding.api.sykmeldingApi

fun Application.configureRouting() {
    val sykmeldingService: SykmeldingService by dependencies

    routing {
        route("/api") {
            sykmeldingApi(sykmeldingService)
        }
    }
}
