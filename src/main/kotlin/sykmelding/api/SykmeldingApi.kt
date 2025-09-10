package no.nav.tsm.sykmelding.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import no.nav.tsm.sykmelding.SykmeldingService
import no.nav.tsm.sykmelding.model.CreateSykmeldingResponse
import no.nav.tsm.sykmelding.model.DollySykmelding
import no.nav.tsm.utils.logger
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Route.sykmeldingApi() {
    val sykmeldingService by inject<SykmeldingService>()
    val log = LoggerFactory.getLogger("no.nav.tsm.sykmelding.api.SykmeldingApiKt")
    post("/sykmelding") {
        logger.info("Oppretter ny sykmelding")

        try {
            val sykmelding = call.receive<DollySykmelding>()

            if (sykmelding.ident.length != 11) {
                call.respond(HttpStatusCode.BadRequest, "fnr må vere 11 siffer. Lengde: ${sykmelding.ident.length}")
                return@post
            }
            val sykmeldingId = sykmeldingService.opprettSykmelding(sykmelding)

            log.info("Opprettet sykmelding med id $sykmeldingId")
            call.respond(HttpStatusCode.OK, CreateSykmeldingResponse(sykmeldingId))
        } catch (e: Exception) {
            log.error("Noe gikk galt ved oppretting av sykmelding", e)
        }

    }
}
