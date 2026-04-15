package no.nav.tsm.sykmelding.model

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

enum class SykmeldingType {
    VANLIG,
    AVVENTENDE,
    BEHANDLINGSDAGER,
    REISETILSKUDD,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Aktivitet(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int? = null,
    val reisetilskudd: Boolean = false,
)

data class DollySykmelding(
    val type: SykmeldingType = SykmeldingType.VANLIG,
    val ident: String,
    val aktivitet: List<Aktivitet>,
)

data class DollySykmeldingResponse(
    val sykmeldingId: String,
    val type: SykmeldingType = SykmeldingType.VANLIG,
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
