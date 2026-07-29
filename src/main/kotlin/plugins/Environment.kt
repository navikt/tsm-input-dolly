package no.nav.tsm.plugins

import io.ktor.server.application.Application
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.nais.getRuntimeCluster
import org.apache.kafka.clients.CommonClientConfigs
import java.util.Properties

class Environment(
    val runtime: RuntimeCluster,
    val kafkaConfig: Properties,
    val sykmeldingTopic: String,
    val jdbcUrl: String,
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName)
        ?: defaultValue
        ?: throw RuntimeException("Missing required variable \"$varName\"")


fun Application.createEnvironment(): Environment {
    val runtime = getRuntimeCluster()

    return Environment(
        runtime = runtime,
        kafkaConfig = when (runtime) {
            RuntimeCluster.LOCAL -> Properties().apply {
                this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
                this[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "PLAINTEXT"
            }

            else -> Properties().apply {
                environment.config.config("kafka.config").toMap().forEach {
                    this[it.key] = it.value
                }
            }
        },
        sykmeldingTopic = "tsm.sykmeldinger",
        jdbcUrl = environment.config.property("database.url").getString(),
    )
}
