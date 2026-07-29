package no.nav.tsm.sykmelding.consumer

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import no.nav.tsm.ktor.logger
import no.nav.tsm.plugins.Environment
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import no.nav.tsm.sykmelding.repository.SykmeldingRepository
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.UUID
import kotlin.collections.set

class SykmeldingParseException(message: String) : Exception(message)

class SykmeldingConsumerService(
    private val repository: SykmeldingRepository,
    private val consumer: Consumer<String, ByteArray>,
    environment: Environment,
) {
    private val logger = logger()
    private val sykmeldingTopic = environment.sykmeldingTopic

    suspend fun start() = coroutineScope {
        logger.info("Starting kafka consumer")
        consumer.subscribe(listOf(sykmeldingTopic))
        try {
            while (isActive) {
                val records = consumer.poll(Duration.ofMillis(1000))
                for (record in records) {
                    processRecord(record)
                }
            }
        } finally {
            consumer.unsubscribe()
            logger.info("Stopped kafka consumer")
        }
    }

    private suspend fun processRecord(record: ConsumerRecord<String, ByteArray>) {
        val sykmeldingId = record.key() ?: UUID.randomUUID().toString()
        val messageValue = record.value()

        try {
            val sykmeldingRecord: SykmeldingRecord? = readValue(messageValue)
            if (sykmeldingRecord == null) {
                logger.info("SykmeldingRecord is null, deleting sykmelding with id: $sykmeldingId")
                repository.deleteBySykmeldingId(sykmeldingId)
            } else {
                val ident = sykmeldingRecord.sykmelding.pasient.fnr
                repository.saveSykmelding(sykmeldingId, ident, sykmeldingRecord)
                logger.info("Successfully processed sykmelding with id: $sykmeldingId")
            }
        } catch (e: SykmeldingParseException) {
            logger.warn("Failed to parse sykmelding with id: $sykmeldingId, saving as corrupt data", e)
            try {
                repository.saveCorruptData(sykmeldingId, messageValue)
            } catch (saveException: Exception) {
                logger.error("Failed to save corrupt data for sykmelding id: $sykmeldingId", saveException)
            }
        } catch (e: Exception) {
            logger.error("Failed to process sykmelding with id: $sykmeldingId", e)
            throw e
        }
    }

    private fun readValue(messageValue: ByteArray?): SykmeldingRecord? {
        try {
            return messageValue?.let { sykmeldingObjectMapper.readValue(messageValue, SykmeldingRecord::class.java) }
        } catch (e: Exception) {
            logger.warn("Failed to parse sykmelding record", e)
            throw SykmeldingParseException("Failed to parse sykmelding record")
        }
    }
}

fun initializeConsumer(env: Environment): Consumer<String, ByteArray> {
    val kafkaProperties = env.kafkaConfig
    kafkaProperties[ConsumerConfig.GROUP_ID_CONFIG] = "tsm-input-dolly"
    kafkaProperties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    kafkaProperties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java
    kafkaProperties[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
    kafkaProperties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
    return KafkaConsumer(kafkaProperties)
}