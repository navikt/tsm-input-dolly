package no.nav.tsm.sykmelding.consumer

import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.tsm.ktor.kafka.test.KafkaContainer
import no.nav.tsm.ktor.kafka.test.send
import no.nav.tsm.pdl.Navn
import no.nav.tsm.plugins.Environment
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import no.nav.tsm.sykmelding.mapper.mapToSykmeldingRecord
import no.nav.tsm.sykmelding.model.Aktivitet
import no.nav.tsm.sykmelding.model.DollySykmelding
import no.nav.tsm.sykmelding.repository.SykmeldingRepository
import java.time.LocalDate
import java.util.*
import kotlin.test.Test

class SykmeldingConsumerTest {

    private val kafka = KafkaContainer(
        createTopics = listOf("tsm.sykmeldinger")
    )
    private val producer = kafka.createAnythingProducer()

    private val testEnv = mockk<Environment>(relaxed = true) { every { runtime.name } returns "test" }
    private val repository: SykmeldingRepository = mockk(relaxed = true)
    private val consumerService: SykmeldingConsumerService = SykmeldingConsumerService(repository)

    private suspend fun ApplicationTestBuilder.testInit() {
        kafka.configureKafka(this)
        application.dependencies {
            provide<Environment> { testEnv }
            provide { consumerService }
        }
        application.configureSykmeldingConsumer()

        startApplication()
    }

    @Test
    fun `should process valid sykmelding record successfully`() = testApplication {
        testInit()

        val sykmeldingId = UUID.randomUUID().toString()
        val ident = "12345678901"
        val dollySykmelding = createTestDollySykmelding(ident)
        val sykmeldingRecord = mapToSykmeldingRecord(sykmeldingId, dollySykmelding, Navn("fornavn", null, "etternavn"))

        producer.send("tsm.sykmeldinger", sykmeldingId, sykmeldingObjectMapper.writeValueAsBytes(sykmeldingRecord))

        coVerify(timeout = 5000) { repository.saveSykmelding(sykmeldingId, ident, any()) }
    }

    @Test
    fun `test tombstone`() = testApplication {
        testInit()

        val sykmeldingId = UUID.randomUUID().toString()
        producer.send("tsm.sykmeldinger", sykmeldingId, null)

        coVerify(exactly = 1, timeout = 5000) { repository.deleteBySykmeldingId(sykmeldingId) }
        coVerify(exactly = 0) { repository.saveSykmelding(any(), any(), any()) }
    }

    @Test
    fun `test 'null' tombstone`() = testApplication {
        testInit()

        val sykmeldingId = UUID.randomUUID().toString()
        val nullBytes: ByteArray = "null".toByteArray()

        producer.send("tsm.sykmeldinger", sykmeldingId, nullBytes)

        coVerify(exactly = 1, timeout = 5000) { repository.deleteBySykmeldingId(sykmeldingId) }
        coVerify(exactly = 0) { repository.saveSykmelding(any(), any(), any()) }
    }

    private fun createTestDollySykmelding(ident: String): DollySykmelding {
        return DollySykmelding(
            ident = ident,
            aktivitet = listOf(
                Aktivitet(
                    fom = LocalDate.of(2025, 9, 10),
                    tom = LocalDate.of(2025, 9, 20)
                )
            )
        )
    }
}
