package no.nav.tsm.plugins

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.application.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import no.nav.tsm.sykmelding.SykmeldingService
import no.nav.tsm.sykmelding.consumer.SykmeldingConsumerService
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import no.nav.tsm.sykmelding.repository.SykmeldingRepository
import no.nav.tsm.texas.TexasClient
import no.nav.tsm.`tsm-pdl`.TsmPdlClient
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun Application.configureKoin() {

    install(Koin) {
        slf4jLogger()
        modules(
            tsmPdlModules(),
            sykmeldingModules()
        )
    }
}

fun tsmPdlModules()  = module {
    single { HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }
    } }
    single {
        TexasClient(get<Environment>().texasUrl, get())
    }
    single {
        val env = get<Environment>()
        TsmPdlClient(get(), get(), env.tsmPdlScope, env.tsmPdlUrl ) }
}

fun Application.sykmeldingModules() = module {
    single {
        createEnvironment()
    }
    single<DataSource> {
        val environment = get<Environment>()
        val url = environment.jdbcUrl

        PGSimpleDataSource().apply {
            setURL(url)
        }
    }

    single<ObjectMapper> {
        sykmeldingObjectMapper
    }

    single { SykmeldingRepository(get(), get()) }

    single {
        val env = get<Environment>()

        if(env.runEnvironment == RunEnvironment.LOCAL) {
            SykmeldingInputKafkaInputFactory.localProducer(
                "tsm-input-dolly",
                "tsm",
                env.kafkaConfig
            )
        } else {
            SykmeldingInputKafkaInputFactory.naisProducer()
        }
    }

    single<Consumer<String, String>> {
        val env = get<Environment>()
        val kafkaProperties = env.kafkaConfig
        kafkaProperties[ConsumerConfig.GROUP_ID_CONFIG] = "tsm-input-dolly-consumer"
        kafkaProperties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        kafkaProperties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java
        kafkaProperties[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        kafkaProperties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        KafkaConsumer(kafkaProperties)
    }

    single { SykmeldingConsumerService(get(), get(), get<Environment>().sykmeldingTopic) }
    single { SykmeldingService(get(), get(), get()) }
}

fun Application.configurefConsumer() {
    val consumerService by inject<SykmeldingConsumerService>()
    val scope = CoroutineScope(Dispatchers.IO)
    monitor.subscribe(ApplicationStarted) {
        log.info("Starting kafka consumer")
        scope.launch {
            while(isActive) {
                try { consumerService.start() }
                catch (ex: Exception) {
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
