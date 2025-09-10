package no.nav.tsm

import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.tsm.sykmelding.api.sykmeldingApi

fun Application.configureRouting() {

    routing {
        route("/api") {
            sykmeldingApi()
        }
    }
}
