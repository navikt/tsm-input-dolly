package no.nav.tsm.sykmelding.consumer

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.ktor.kafka.sykmeldinger.SykmeldingerConsumer
import no.nav.tsm.plugins.Environment

fun Application.configureSykmeldingConsumer() {
    val env: Environment by dependencies
    val consumerService: SykmeldingConsumerService by dependencies

    install(SykmeldingerConsumer) {
        clientId = env.runtime.name
        groupId = "tsm-input-dolly"
        pollDuration = env.consumer.poll
        retryDuration = env.consumer.retry
        onRecord = { record, _ ->
            consumerService.handleSykmelding(record)
        }
        onTombstone = {
            consumerService.handleTombstone(it.key)
        }
    }
}
