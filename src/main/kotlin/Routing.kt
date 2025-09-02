package no.nav.tsm

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.tsm.sykmelding.SykmeldingServiceTest
import no.nav.tsm.sykmelding.api.sykmeldingApi

fun Application.configureRouting(
    sykmeldingServiceTest: SykmeldingServiceTest
) {
    routing {
        get("/") {
            call.respondText("Dummy GET! :)")
        }
        post("/test") {
            try {
                call.respond(sykmeldingServiceTest.test())
            } catch (e: Exception) {
                call.respondText("Error: ${e.message}", status = io.ktor.http.HttpStatusCode.InternalServerError)
            }
        }
        route("/api") {
            sykmeldingApi()
        }
    }
}
