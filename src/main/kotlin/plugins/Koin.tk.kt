package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.tsm.sykmelding.SykmeldingService
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import org.apache.kafka.clients.CommonClientConfigs
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import java.util.Properties

fun Application.configureKoin() {

    install(Koin) {
        slf4jLogger()
        modules(
            sykmeldingModules()
        )
    }
}

fun Application.sykmeldingModules() = module {
    single {
        if(developmentMode) {
            SykmeldingInputKafkaInputFactory.localProducer(
                "tsm-input-dolly",
                "tsm",
                Properties().apply {
                    this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
                    this["security.protocol"] = "PLAINTEXT"
                }
            )
        } else {
            SykmeldingInputKafkaInputFactory.naisProducer()
        }
    }

    single { SykmeldingService(get()) }
}
