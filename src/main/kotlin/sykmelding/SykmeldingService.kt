package no.nav.tsm.sykmelding

import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
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
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonId
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import no.nav.tsm.sykmelding.model.DollySykmelding
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class SykmeldingService(private val sykmeldingProducer: SykmeldingInputProducer) {
    fun opprettSykmelding(sykmelding: DollySykmelding): String {
        val sykmeldingId = UUID.randomUUID().toString()

        val sykmeldingRecord = mapToSykmeldingRecord(sykmeldingId, sykmelding)

        sykmeldingProducer.sendSykmelding(sykmeldingRecord)

        return sykmeldingId
    }

    fun mapToSykmeldingRecord(sykmeldingId: String, sykmelding: DollySykmelding): SykmeldingRecord {
        return SykmeldingRecord(
            metadata = Digital("223456789"),
            sykmelding = DigitalSykmelding(
                id = sykmeldingId,
                metadata = DigitalSykmeldingMetadata(
                    mottattDato = OffsetDateTime.now(ZoneOffset.UTC),
                    genDate = OffsetDateTime.now(ZoneOffset.UTC),
                ),
                pasient = Pasient(
                    fnr = sykmelding.ident,
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
                aktivitet = listOf(
                    AktivitetIkkeMulig(
                        fom = sykmelding.aktivitet.fom,
                        tom = sykmelding.aktivitet.tom,
                        medisinskArsak = null,
                        arbeidsrelatertArsak = null
                    )
                ),
                behandler = Behandler(
                    navn = Navn(fornavn = "Ola", mellomnavn = "Norman", etternavn = "Hansen"),
                    adresse = null,
                    ids = listOf(PersonId(type = PersonIdType.HPR, id = "9144889")),
                    kontaktinfo = emptyList(),
                ),
                sykmelder = Sykmelder(
                    ids = listOf(PersonId(type = PersonIdType.HPR, id = "9144889")),
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
