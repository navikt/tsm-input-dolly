package no.nav.tsm.sykmelding.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import no.nav.tsm.sykmelding.moduel.Sykmelding
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import no.nav.tsm.sykmelding.SykmeldingService
import no.nav.tsm.utils.logger
import org.koin.ktor.ext.inject

fun Route.sykmeldingApi() {
    val sykmeldingService by inject<SykmeldingService>()

    post("/sykmelding/opprett") {
        val sykmelding = call.receive<Sykmelding>()

        if (sykmelding.fnr.length != 11) {
            call.respond(HttpStatusCode.BadRequest, "fnr må vere 11 siffer. Lengde: ${sykmelding.fnr.length}")
            return@post
        }

        val sykmeldingId = sykmeldingService.opprettSykmelding(sykmelding)

        logger.info("Opprettet sykmelding med id $sykmeldingId")
        call.respond(HttpStatusCode.OK, "Opprettet sykmelding med id $sykmeldingId")
    }
}
