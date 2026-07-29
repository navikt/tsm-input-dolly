package no.nav.tsm.plugins

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import no.nav.tsm.ktor.di.dynamicDependencies
import no.nav.tsm.sykmelding.SykmeldingService
import no.nav.tsm.sykmelding.consumer.SykmeldingConsumerService
import no.nav.tsm.sykmelding.consumer.initializeConsumer
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import no.nav.tsm.sykmelding.repository.SykmeldingRepository
import no.nav.tsm.texas.TexasClient
import no.nav.tsm.`tsm-pdl`.TsmPdlClient
import org.apache.kafka.clients.consumer.Consumer
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun Application.configureDependencies() {
    val env = createEnvironment()
    dependencies {
        provide<Environment> { env }
        provide<HttpClient> { configureBaseHttpClient() }
        provide(TexasClient::class)
        provide(TsmPdlClient::class)
        provide<DataSource> {
            PGSimpleDataSource().apply { setURL(env.jdbcUrl) }
        }
        provide<Consumer<String, ByteArray>> { initializeConsumer(env) }
        provide(SykmeldingRepository::class)
        provide(SykmeldingConsumerService::class)
        provide(SykmeldingService::class)

    }

    dynamicDependencies {
        cloud {
            provide<SykmeldingInputProducer> {
                SykmeldingInputKafkaInputFactory.naisProducer()
            }
        }
        local {
            provide<SykmeldingInputProducer> {
                SykmeldingInputKafkaInputFactory.localProducer(
                    "tsm-input-dolly",
                    "tsm",
                    env.kafkaConfig
                )
            }
        }
    }
}

private fun configureBaseHttpClient(): HttpClient = HttpClient(Apache5) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
}

fun Application.configureConsumer() {
    val consumerService: SykmeldingConsumerService by dependencies

    val scope = CoroutineScope(Dispatchers.IO)
    monitor.subscribe(ApplicationStarted) {
        log.info("Starting kafka consumer")
        scope.launch {
            while (isActive) {
                try {
                    consumerService.start()
                } catch (ex: Exception) {
                    log.error("Error running consumer", ex)
                    delay(10.seconds.toJavaDuration())
                }
            }
        }
    }

    monitor.subscribe(ApplicationStopping) {
        log.info("Stopping kafka consumer")
        scope.cancel()
    }

}
