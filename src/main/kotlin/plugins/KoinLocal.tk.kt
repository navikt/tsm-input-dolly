package no.nav.tsm.plugins

import no.nav.tsm.sykmelding.SykmeldingService
import org.koin.core.KoinApplication
import org.koin.dsl.module

fun KoinApplication.initDevelopmentModules() {
    modules(
        developmentSykmeldingModule,
    )
}

val developmentSykmeldingModule = module { single { SykmeldingService() } }
