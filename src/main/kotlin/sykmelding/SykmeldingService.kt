package no.nav.tsm.sykmelding

import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmeldingMetadata
import no.nav.tsm.sykmelding.input.core.model.IngenArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Sykmelder
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.metadata.Digital
import no.nav.tsm.sykmelding.input.core.model.metadata.HelsepersonellKategori
import no.nav.tsm.sykmelding.input.core.model.metadata.Navn
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import no.nav.tsm.sykmelding.moduel.Sykmelding
import java.time.OffsetDateTime
import java.util.UUID

class SykmeldingService() {
    fun opprettSykmelding(sykmelding: Sykmelding): String {
        val sykmeldingId = UUID.randomUUID().toString()

        val sykmeldingRecord = mapToSykmeldingRecord(sykmeldingId, sykmelding)

        val producer = SykmeldingInputKafkaInputFactory.naisProducer()
        producer.sendSykmelding(sykmeldingRecord)

        return sykmeldingId
    }

    fun mapToSykmeldingRecord(sykmeldingId: String, sykmelding: Sykmelding): SykmeldingRecord {
        return SykmeldingRecord(
            metadata = Digital("169"),
            sykmelding = DigitalSykmelding(
                id = sykmeldingId,
                metadata = DigitalSykmeldingMetadata(
                    mottattDato = OffsetDateTime.now(),
                    genDate = OffsetDateTime.now(),
                ),
                pasient = Pasient(
                    fnr = sykmelding.fnr,
                    navn = null,
                    navKontor = null,
                    navnFastlege = null,
                    kontaktinfo = emptyList()
                ),
                medisinskVurdering = MedisinskVurdering(
                    hovedDiagnose = null,
                    biDiagnoser = null,
                    yrkesskade = null,
                    syketilfelletStartDato = null,
                    annenFraversArsak = null,
                    svangerskap = false,
                    skjermetForPasient = false,
                ),
                aktivitet = sykmelding.aktivitet,
                behandler = Behandler(
                    navn = Navn(fornavn = "Ola", mellomnavn = "Norman", etternavn = "Hansen"),
                    adresse = null,
                    ids = emptyList(),
                    kontaktinfo = emptyList(),
                ),
                sykmelder = Sykmelder(
                    ids = emptyList(),
                    helsepersonellKategori = HelsepersonellKategori.LEGE,
                ),
                arbeidsgiver = IngenArbeidsgiver(),
                tilbakedatering = null,
                bistandNav = null,
            ),
            validation = ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now(),
                rules = emptyList(),
            )
        )
    }
}
