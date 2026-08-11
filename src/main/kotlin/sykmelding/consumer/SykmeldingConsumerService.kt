package no.nav.tsm.sykmelding.consumer

import no.nav.tsm.ktor.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.repository.SykmeldingRepository

class SykmeldingConsumerService(
    private val repository: SykmeldingRepository,
) {
    private val logger = logger()

    suspend fun handleSykmelding(record: SykmeldingRecord) {
        val sykmeldingId = record.sykmelding.id

        try {
            val ident = record.sykmelding.pasient.fnr
            repository.saveSykmelding(sykmeldingId, ident, record)
            logger.info("Successfully processed sykmelding with id: $sykmeldingId")
        } catch (e: Exception) {
            logger.error("Failed to process sykmelding with id: $sykmeldingId", e)
            throw e
        }
    }

    suspend fun handleTombstone(id: String) {
        logger.info("SykmeldingRecord is null, deleting sykmelding with id: $id")
        repository.deleteBySykmeldingId(id)
    }
}
