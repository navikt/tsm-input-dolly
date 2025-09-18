package no.nav.tsm.sykmelding.model
import java.time.LocalDate

data class Aktivitet(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int? = null,
)

data class DollySykmelding(
    val ident: String,
    val aktivitet: List<Aktivitet>,
)

data class DollySykmeldingResponse(
    val sykmeldingId: String,
    val ident: String,
    val aktivitet: List<Aktivitet>,
)

data class DollySykmeldingerResponse(
    val sykmeldinger: List<DollySykmeldingResponse>
)


data class SykmeldingNotFound(
    private val id: String,
    val message: String = "Sykmelding not found $id"
)

data class ErrorMessage(
    val message: String
)
