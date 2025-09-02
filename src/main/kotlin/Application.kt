package no.nav.tsm

import io.ktor.server.application.*
import no.nav.tsm.kafka.initLocalKafka
import no.nav.tsm.plugins.configureKoin
import no.nav.tsm.sykmelding.SykmeldingServiceTest
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val producer = if (this.developmentMode) initLocalKafka() else SykmeldingInputKafkaInputFactory.naisProducer()
    val sykmeldingServiceTest = SykmeldingServiceTest(producer = producer)

    configureKoin()
    configureHealthChecks()
    configureAdministration()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureRouting(sykmeldingServiceTest)
}
