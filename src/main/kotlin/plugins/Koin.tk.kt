package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.tsm.sykmelding.SykmeldingService
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        initProductionModules()
        if (developmentMode) {
            initDevelopmentModules()
        }
    }
}

fun KoinApplication.initProductionModules() {
    modules(
        sykmeldingModule,
    )
}

val sykmeldingModule = module { single { SykmeldingService() } }
