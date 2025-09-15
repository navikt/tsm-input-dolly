package no.nav.tsm.sykmelding.model
import java.time.LocalDate

data class Aktivitet(
    val fom: LocalDate,
    val tom: LocalDate,
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

data class InternalServerError(
    private val id: String,
    val message: String = "Error while getting sykmelding with id: $id"
)
