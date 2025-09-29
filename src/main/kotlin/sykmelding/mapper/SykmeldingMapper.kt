package no.nav.tsm.sykmelding.mapper

import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmeldingMetadata
import no.nav.tsm.sykmelding.input.core.model.Gradert
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
import no.nav.tsm.sykmelding.model.Aktivitet
import no.nav.tsm.sykmelding.model.DollySykmelding
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun mapToSykmeldingRecord(sykmeldingId: String, sykmelding: DollySykmelding, navn: no.nav.tsm.`tsm-pdl`.Navn): SykmeldingRecord {
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
                navn = Navn(fornavn = navn.fornavn, mellomnavn = navn.mellomnavn, etternavn = navn.etternavn),
                navKontor = null,
                navnFastlege = null,
                kontaktinfo = emptyList()
            ),
            medisinskVurdering = MedisinskVurdering(
                hovedDiagnose = DiagnoseInfo(
                    DiagnoseSystem.ICPC2,
                    kode = "R80",
                    tekst = "Influensa"
                ),
                biDiagnoser = null,
                yrkesskade = null,
                syketilfelletStartDato = null,
                annenFraversArsak = null,
                svangerskap = false,
                skjermetForPasient = false,
            ),
            aktivitet = sykmelding.aktivitet.map {
                toAktivitet(it)
            },
            behandler = Behandler(
                navn = Navn(fornavn = "Overmodig", mellomnavn = null, etternavn = "Jekk"),
                adresse = null,
                ids = listOf(PersonId(type = PersonIdType.HPR, id = "565562871"), PersonId(type = PersonIdType.FNR, id = "16889298166")),
                kontaktinfo = emptyList(),
            ),
            sykmelder = Sykmelder(
                ids = listOf(PersonId(type = PersonIdType.HPR, id = "565562871"), PersonId(type = PersonIdType.FNR, id = "16889298166")),
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

private fun toAktivitet(aktivitet: Aktivitet): no.nav.tsm.sykmelding.input.core.model.Aktivitet {
    return when (aktivitet.grad) {
        null -> AktivitetIkkeMulig(
            fom = aktivitet.fom,
            tom = aktivitet.tom,
            medisinskArsak = null,
            arbeidsrelatertArsak = null
        )

        else -> Gradert(
            grad = aktivitet.grad,
            fom = aktivitet.fom,
            tom = aktivitet.tom,
            reisetilskudd = false,
        )
    }
}
