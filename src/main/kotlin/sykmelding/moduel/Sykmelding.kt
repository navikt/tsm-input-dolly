package no.nav.tsm.sykmelding.moduel

import no.nav.tsm.sykmelding.input.core.model.Aktivitet

data class Sykmelding(
    val fnr: String,
    val aktivitet: List<Aktivitet>,
)
