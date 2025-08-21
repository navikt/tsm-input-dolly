package no.nav.tsm.kafka

import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import org.apache.kafka.clients.CommonClientConfigs
import java.util.Properties
import kotlin.collections.set


fun initLocalKafka(): SykmeldingInputProducer {
    return SykmeldingInputKafkaInputFactory.localProducer(
        "tsm-input-dolly",
        "tsm",
        Properties().apply {
            this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
            this["security.protocol"] = "PLAINTEXT"
        }

    )
}