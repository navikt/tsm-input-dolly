package no.nav.tsm.sykmelding

import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmeldingMetadata
import no.nav.tsm.sykmelding.input.core.model.IngenArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.MedisinskArsak
import no.nav.tsm.sykmelding.input.core.model.MedisinskArsakType
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Sykmelder
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.metadata.Digital
import no.nav.tsm.sykmelding.input.core.model.metadata.HelsepersonellKategori
import no.nav.tsm.sykmelding.input.core.model.metadata.Navn
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import java.time.LocalDate
import java.time.OffsetDateTime

class SykmeldingService(val producer: SykmeldingInputProducer) {
    fun test(): Map<String, Any> {
        producer.sendSykmelding(
            SykmeldingRecord(
                metadata = Digital("169"),
                sykmelding = DigitalSykmelding(
                    id = "test-sykmelding-id",
                    metadata = DigitalSykmeldingMetadata(
                        mottattDato = OffsetDateTime.now(),
                        genDate = OffsetDateTime.now(),
                    ),
                    pasient = Pasient(
                        fnr = "12345678901",
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
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 1, 31),
                            medisinskArsak = MedisinskArsak(
                                beskrivelse = null,
                                arsak = listOf(MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET)
                            ),
                            arbeidsrelatertArsak = null
                        )
                    ),
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
        )

        return mapOf("test" to true)
    }
}
