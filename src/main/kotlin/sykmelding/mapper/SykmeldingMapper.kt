package no.nav.tsm.sykmelding.mapper

import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.input.core.model.AvsenderSystem
import no.nav.tsm.sykmelding.input.core.model.Avventende
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.Behandlingsdager
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmeldingMetadata
import no.nav.tsm.sykmelding.input.core.model.Gradert
import no.nav.tsm.sykmelding.input.core.model.IngenArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.Reisetilskudd
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
import no.nav.tsm.sykmelding.model.SykmeldingType
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
                avsenderSystem = AvsenderSystem("Dolly", "2"),
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
            aktivitet = mapAktivitet(sykmelding),
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

private fun mapAktivitet(sykmelding: DollySykmelding): List<no.nav.tsm.sykmelding.input.core.model.Aktivitet> {
    return when (sykmelding.type) {
        SykmeldingType.VANLIG -> sykmelding.aktivitet.map { toVanligAktivitet(it) }
        SykmeldingType.AVVENTENDE -> listOf(
            Avventende(
                innspillTilArbeidsgiver = "Ikke spesifisert inspill",
                fom = sykmelding.aktivitet.first().fom,
                tom = sykmelding.aktivitet.first().tom,
            )
        )
        SykmeldingType.BEHANDLINGSDAGER -> listOf(
            Behandlingsdager(
                antallBehandlingsdager = 1,
                fom = sykmelding.aktivitet.first().fom,
                tom = sykmelding.aktivitet.first().tom,
            )
        )
        SykmeldingType.REISETILSKUDD -> listOf(
            Reisetilskudd(
                fom = sykmelding.aktivitet.first().fom,
                tom = sykmelding.aktivitet.first().tom,
            )
        )
    }
}

private fun toVanligAktivitet(aktivitet: Aktivitet): no.nav.tsm.sykmelding.input.core.model.Aktivitet {
    return if (aktivitet.grad != null) {
        Gradert(
            grad = aktivitet.grad,
            fom = aktivitet.fom,
            tom = aktivitet.tom,
            reisetilskudd = aktivitet.reisetilskudd,
        )
    } else {
        AktivitetIkkeMulig(
            fom = aktivitet.fom,
            tom = aktivitet.tom,
            medisinskArsak = null,
            arbeidsrelatertArsak = null,
        )
    }
}
