package no.nav.tsm

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.tsm.sykmelding.SykmeldingService

fun Application.configureRouting(
    sykmeldingService: SykmeldingService
) {
    routing {
        get("/") {
            call.respondText("Dummy GET! :)")
        }
        post("/test") {
            call.respond(sykmeldingService.test())
        }
    }
}
