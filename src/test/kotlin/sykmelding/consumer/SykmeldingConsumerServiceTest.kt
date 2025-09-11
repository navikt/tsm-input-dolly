package no.nav.tsm.sykmelding.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import no.nav.tsm.sykmelding.mapper.mapToSykmeldingRecord
import no.nav.tsm.sykmelding.model.Aktivitet
import no.nav.tsm.sykmelding.model.DollySykmelding
import no.nav.tsm.sykmelding.repository.SykmeldingRepository
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.mapOf
import kotlin.test.BeforeTest
import kotlin.test.Test

class SykmeldingConsumerServiceTest {

    private lateinit var consumer: Consumer<String, ByteArray>
    private lateinit var repository: SykmeldingRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var consumerService: SykmeldingConsumerService

    @BeforeTest
    fun setup() {
        consumer = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        objectMapper = sykmeldingObjectMapper
        consumerService = SykmeldingConsumerService(consumer, repository, "tsm.sykmeldinger")
    }

    @Test
    fun `should process valid sykmelding record successfully`() = runTest {
        val sykmeldingId = UUID.randomUUID().toString()
        val ident = "12345678901"
        val dollySykmelding = createTestDollySykmelding(ident)
        val sykmeldingRecord = mapToSykmeldingRecord(sykmeldingId, dollySykmelding)
        val json = objectMapper.writeValueAsBytes(sykmeldingRecord)

        val consumerRecord = ConsumerRecord(
            "tsm.sykmeldinger",
            0,
            0L,
            sykmeldingId,
            json
        )

        runConsumer(consumerRecord)

        verify { consumer.subscribe(listOf("tsm.sykmeldinger")) }
        coVerify { repository.saveSykmelding(sykmeldingId, ident, any()) }
    }

    @Test
    fun `test tombstone`() = runTest {
        val sykmeldingId = UUID.randomUUID().toString()
        val invalidJson: ByteArray? = null

        val consumerRecord = ConsumerRecord<String, ByteArray>(
            "tsm.sykmeldinger",
            0,
            0L,
            sykmeldingId,
            invalidJson
        )

        runConsumer(consumerRecord)


        verify { consumer.subscribe(listOf("tsm.sykmeldinger")) }
        coVerify(exactly = 1) { repository.deleteBySykmeldingId(sykmeldingId)  }
        coVerify(exactly = 0) { repository.saveSykmelding(any(), any(), any()) }
        coVerify(exactly = 0) { repository.saveCorruptData(any(), any()) }
    }

    @Test
    fun `test 'null' tombstone`() = runTest {
        val sykmeldingId = UUID.randomUUID().toString()
        val invalidJson: ByteArray = "null".toByteArray()

        val consumerRecord = ConsumerRecord(
            "tsm.sykmeldinger",
            0,
            0L,
            sykmeldingId,
            invalidJson
        )

        runConsumer(consumerRecord)


        verify { consumer.subscribe(listOf("tsm.sykmeldinger")) }
        coVerify(exactly = 1) { repository.deleteBySykmeldingId(sykmeldingId)  }
        coVerify(exactly = 0) { repository.saveSykmelding(any(), any(), any()) }
        coVerify(exactly = 0) { repository.saveCorruptData(any(), any()) }
    }

    @Test
    fun `should save corrupt data when parsing fails`() = runTest {
        val sykmeldingId = UUID.randomUUID().toString()
        val invalidJson = "{sykmelding: sykmelding}".toByteArray()

        val consumerRecord = ConsumerRecord(
            "tsm.sykmeldinger",
            0,
            0L,
            sykmeldingId,
            invalidJson
        )

        runConsumer(consumerRecord)


        verify { consumer.subscribe(listOf("tsm.sykmeldinger")) }
        coVerify { repository.saveCorruptData(sykmeldingId, invalidJson) }
    }

    private fun createTestDollySykmelding(ident: String): DollySykmelding {
        return DollySykmelding(
            ident = ident,
            aktivitet = Aktivitet(
                fom = LocalDate.of(2025, 9, 10),
                tom = LocalDate.of(2025, 9, 20)
            )
        )
    }


    private suspend fun TestScope.runConsumer(consumerRecord: ConsumerRecord<String, ByteArray>) {
        val consumerRecords = ConsumerRecords(
            mapOf(TopicPartition("tsm.sykmeldinger", 0) to listOf(consumerRecord)), mapOf()
        )

        val consumerJob = launch { consumerService.start() }

        every { consumer.poll(any<Duration>()) } answers
                {
                    consumerRecords
                } andThenAnswer {
            consumerJob.cancel()
            ConsumerRecords.empty()
        }
        consumerJob.join()
    }
}


