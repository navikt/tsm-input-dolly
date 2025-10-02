package no.nav.tsm.sykmelding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tsm.sykmelding.exceptions.SykmeldingValidationException
import no.nav.tsm.sykmelding.input.core.model.Gradert
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import no.nav.tsm.sykmelding.mapper.mapToSykmeldingRecord
import no.nav.tsm.sykmelding.model.Aktivitet
import no.nav.tsm.sykmelding.model.DollySykmelding
import no.nav.tsm.sykmelding.model.DollySykmeldingResponse
import no.nav.tsm.sykmelding.model.DollySykmeldingerResponse
import no.nav.tsm.sykmelding.repository.SykmeldingRepository
import no.nav.tsm.`tsm-pdl`.TsmPdlClient
import no.nav.tsm.`tsm-pdl`.TsmPdlResponse
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

class SykmeldingService(private val sykmeldingProducer: SykmeldingInputProducer, private val sykmeldingRepository: SykmeldingRepository, private val tsmPdlClient: TsmPdlClient) {

    suspend fun opprettSykmelding(sykmelding: DollySykmelding): String {
        if (sykmelding.ident.length != 11) {
            throw SykmeldingValidationException("fnr må vere 11 siffer. Lengde: ${sykmelding.ident.length}")
        }
        val person = tsmPdlClient.getPerson(sykmelding.ident)
        validateSykmelding(sykmelding, person)
        val personNavn = person?.navn ?: throw SykmeldingValidationException("Personen har ikke navn i pdl")
        val sykmeldingId = UUID.randomUUID().toString()

        val sykmeldingRecord = mapToSykmeldingRecord(sykmeldingId, sykmelding, personNavn)

        sykmeldingRepository.saveSykmelding(sykmeldingId, sykmelding.ident, sykmeldingRecord)
        withContext(Dispatchers.IO) {
            sykmeldingProducer.sendSykmelding(sykmeldingRecord)
        }

        return sykmeldingId
    }

    private fun validateSykmelding(sykmelding: DollySykmelding, person: TsmPdlResponse?) {

        if (person == null) {
            throw SykmeldingValidationException("Fant ikke person i PDL")
        }
        if (person.navn == null) {
            throw SykmeldingValidationException("Personen har ikke navn i pdl")
        }
        if (person.falskIdent || person.doed) {
            throw SykmeldingValidationException("Personen er doed eller har falsk ident")
        }

        if (sykmelding.aktivitet.any { it.grad != null && it.grad !in 1..99 }) {
            throw SykmeldingValidationException("Grad must be in rage 1-99")
        }
        var lastTom: LocalDate? = null
        sykmelding.aktivitet.sortedBy {
            it.fom
        }.forEach { aktivitet ->
            if (aktivitet.fom.isAfter(aktivitet.tom)) {
                throw SykmeldingValidationException("Fom ${aktivitet.fom} must be equal or before ${aktivitet.tom}")
            }
            if (lastTom != null && !aktivitet.fom.isAfter(lastTom)) {
                throw SykmeldingValidationException("Fom: ${aktivitet.fom} in aktivitet must be after tom $lastTom in previous aktivitet")
            }

            val fomMax = when (lastTom?.dayOfWeek) {
                DayOfWeek.FRIDAY -> lastTom.plusDays(3)
                DayOfWeek.SATURDAY -> lastTom.plusDays(2)
                else -> lastTom?.plusDays(1) ?: aktivitet.fom
            }

            if (aktivitet.fom.isAfter(fomMax)) {
                throw SykmeldingValidationException("Fom: ${aktivitet.fom} in aktivitet can not be after $fomMax")
            }
            lastTom = aktivitet.tom
        }
    }

    suspend fun hentSykmelding(sykmeldingId: String): DollySykmeldingResponse? {
        val sykmeldingRecord = sykmeldingRepository.getById(sykmeldingId) ?: return null
        return toDollySykmelding(sykmeldingRecord)
    }

    private fun toDollySykmelding(sykmeldingRecord: SykmeldingRecord): DollySykmeldingResponse = DollySykmeldingResponse(
        sykmeldingId = sykmeldingRecord.sykmelding.id,
        ident = sykmeldingRecord.sykmelding.pasient.fnr,
        aktivitet = sykmeldingRecord.sykmelding.aktivitet.map {
            Aktivitet(
                fom = it.fom,
                tom = it.tom,
                grad =if(it is Gradert) {
                    it.grad
                } else null
            )
        }
    )

    suspend fun hentSykmeldingByIdent(ident: String): DollySykmeldingerResponse {
        val sykmeldinger = sykmeldingRepository.getByIdent(ident).map { sykmeldingRecord ->
            toDollySykmelding(sykmeldingRecord)
        }
        return DollySykmeldingerResponse(sykmeldinger)
    }

    suspend fun deleteSykmeldingerForIdent(ident: String) {
        val sykmeldinger = sykmeldingRepository.getByIdent(ident).map { it.sykmelding.id }
        withContext(Dispatchers.IO) {
            sykmeldinger.forEach { sykmeldingId ->
                sykmeldingProducer.tombstoneSykmelding(sykmeldingId)
            }
        }
    }
}
