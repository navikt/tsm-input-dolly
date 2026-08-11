package no.nav.tsm

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import io.mockk.mockk
import no.nav.tsm.ktor.auth.texas.Texas
import no.nav.tsm.ktor.kafka.test.KafkaContainer
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.plugins.ConsumerConfig
import no.nav.tsm.plugins.Environment
import no.nav.tsm.plugins.Runtime
import no.nav.tsm.sykmelding.testcontainers.PostgresSQL.Companion.postgres
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class ApplicationTest {

    val kafka = KafkaContainer(
        createTopics = listOf("tsm.sykmeldinger")
    )

    @Test
    fun testRoot() = testApplication {
        kafka.configureKafka(this)

        application {
            dependencies {
                provide<Environment> {
                    Environment(
                        runtime = Runtime(RuntimeCluster.DEV, "testy-app"),
                        consumer = ConsumerConfig(
                            poll = 1.seconds,
                            retry = 1.seconds
                        ),
                        jdbcUrl = postgres.jdbcUrl
                    )
                }
                provide<Texas> { mockk() }
            }
            module()
        }

        client.get("/internal/health/alive").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

}


