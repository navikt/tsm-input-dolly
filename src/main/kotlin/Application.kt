package no.nav.tsm

import io.ktor.server.application.*
import no.nav.tsm.sykmelding.SykmeldingService
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import org.apache.kafka.clients.CommonClientConfigs
import java.util.Properties

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val producer: SykmeldingInputProducer = if (this.developmentMode) SykmeldingInputKafkaInputFactory.localProducer(
        "tsm-input-dolly",
        "tsm",
        Properties().apply {
            this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
            this["security.protocol"] = "PLAINTEXT"
        }

    ) else SykmeldingInputKafkaInputFactory.naisProducer()
    val sykmeldingService = SykmeldingService(producer = producer)

    configureHealthChecks()
    configureAdministration()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureRouting(sykmeldingService)
}
