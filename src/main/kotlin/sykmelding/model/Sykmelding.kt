package no.nav.tsm.sykmelding.model
import java.time.LocalDate

data class Aktivitet(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class DollySykmelding(
    val ident: String,
    val aktivitet: Aktivitet,
)
