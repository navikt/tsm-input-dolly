package no.nav.tsm

import io.ktor.server.application.*
import no.nav.tsm.kafka.initLocalKafka
import no.nav.tsm.sykmelding.SykmeldingService
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val producer = if (this.developmentMode) initLocalKafka() else SykmeldingInputKafkaInputFactory.naisProducer()
    val sykmeldingService = SykmeldingService(producer = producer)

    configureHealthChecks()
    configureAdministration()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureRouting(sykmeldingService)
}
