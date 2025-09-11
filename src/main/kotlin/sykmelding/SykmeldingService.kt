package no.nav.tsm.sykmelding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import no.nav.tsm.sykmelding.mapper.mapToSykmeldingRecord
import no.nav.tsm.sykmelding.model.Aktivitet
import no.nav.tsm.sykmelding.model.DollySykmelding
import no.nav.tsm.sykmelding.model.DollySykmeldingResponse
import no.nav.tsm.sykmelding.model.DollySykmeldingerResponse
import no.nav.tsm.sykmelding.repository.SykmeldingRepository
import java.util.UUID

class SykmeldingService(private val sykmeldingProducer: SykmeldingInputProducer, private val sykmeldingRepository: SykmeldingRepository) {
    suspend fun opprettSykmelding(sykmelding: DollySykmelding): String {
        val sykmeldingId = UUID.randomUUID().toString()

        val sykmeldingRecord = mapToSykmeldingRecord(sykmeldingId, sykmelding)

        sykmeldingRepository.saveSykmelding(sykmeldingId, sykmelding.ident, sykmeldingRecord)
        withContext(Dispatchers.IO) {
            sykmeldingProducer.sendSykmelding(sykmeldingRecord)
        }

        return sykmeldingId
    }

    suspend fun hentSykmelding(sykmeldingId: String): DollySykmeldingResponse? {
        val sykmeldingRecord = sykmeldingRepository.getById(sykmeldingId) ?: return null
        return toDollySykmelding(sykmeldingRecord)
    }

    private fun toDollySykmelding(sykmeldingRecord: SykmeldingRecord): DollySykmeldingResponse = DollySykmeldingResponse(
        sykmeldingId = sykmeldingRecord.sykmelding.id,
        ident = sykmeldingRecord.sykmelding.pasient.fnr,
        aktivitet = Aktivitet(
            fom = sykmeldingRecord.sykmelding.aktivitet.first().fom,
            tom = sykmeldingRecord.sykmelding.aktivitet.first().tom
        )
    )

    suspend fun hentSykmeldingByIdent(ident: String): DollySykmeldingerResponse {
        val sykmeldinger = sykmeldingRepository.getByIdent(ident).map { sykmeldingRecord ->
            toDollySykmelding(sykmeldingRecord)
        }
        return DollySykmeldingerResponse(sykmeldinger)
    }
}
