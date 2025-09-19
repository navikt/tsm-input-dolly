package no.nav.tsm.sykmelding.api

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.tsm.sykmelding.SykmeldingService
import no.nav.tsm.sykmelding.exceptions.SykmeldingValidationException
import no.nav.tsm.sykmelding.model.DollySykmelding
import no.nav.tsm.sykmelding.model.DollySykmeldingResponse
import no.nav.tsm.sykmelding.model.ErrorMessage
import no.nav.tsm.sykmelding.model.SykmeldingNotFound
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

            val sykmeldingId = sykmeldingService.opprettSykmelding(sykmelding)

            log.info("Opprettet sykmelding med id $sykmeldingId")
            call.respond(
                HttpStatusCode.OK, DollySykmeldingResponse(
                    sykmeldingId = sykmeldingId,
                    ident = sykmelding.ident,
                    aktivitet = sykmelding.aktivitet
                )
            )
        } catch (ex: SykmeldingValidationException) {
            logger.error("Validering av sykmelding feilet: ${ex.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorMessage(ex.message ?: "Validering av sykmelding feilet")
            )
        }
        catch (e: Exception) {
            logger.error("Noe gikk galt ved oppretting av sykmelding", e)
            call.respond(InternalServerError, ErrorMessage("Noe gikk galt ved oppretting av sykmelding"))
        }

    }
    get("/sykmelding/{sykmeldingId}") {
        val sykmeldingId = call.parameters["sykmeldingId"]
        requireNotNull(sykmeldingId)
        logger.info("Henter sykmelding med id ${call.parameters["sykmeldingId"]}")

        try {
            val dollySykmeldingResponse = sykmeldingService.hentSykmelding(sykmeldingId)
            if (dollySykmeldingResponse == null) {
                call.respond(HttpStatusCode.NotFound, SykmeldingNotFound(sykmeldingId))
            } else {
                call.respond(HttpStatusCode.OK, dollySykmeldingResponse)
            }
        } catch (e: Exception) {
            logger.error("Noe gikk galt ved henting av sykmelding med id $sykmeldingId", e)
            call.respond(InternalServerError, ErrorMessage("Error while getting sykmelding with id: $sykmeldingId"))
        }
    }

    get("/sykmelding/ident") {
        val ident = call.request.headers["X-ident"]
        requireNotNull(ident)
        logger.info("Henter sykmeldinger for ident")

        try {
            val response = sykmeldingService.hentSykmeldingByIdent(ident)
            call.respond(HttpStatusCode.OK, response)
        } catch (e: Exception) {
            logger.error("Noe gikk galt ved henting av sykmeldinger for ident", e)
            call.respond(InternalServerError, ErrorMessage("Error while getting sykmeldinger for ident"))
        }
    }
    delete("/sykmelding/ident") {
        val ident = call.request.headers["X-ident"]
        requireNotNull(ident)
        logger.info("Sletter alle sykmeldinger for ident")

        try {
            sykmeldingService.deleteSykmeldingerForIdent(ident)
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            logger.error("Noe gikk galt ved sletting av sykmeldinger for ident", e)
            call.respond(InternalServerError, ErrorMessage("Error while deleting sykmelding for ident"))
        }

    }
}

