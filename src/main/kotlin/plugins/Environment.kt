package no.nav.tsm.plugins

import io.ktor.server.application.Application
import org.apache.kafka.clients.CommonClientConfigs
import java.util.Properties

enum class RunEnvironment {
    LOCAL, DEV, PROD
}

class Environment (
    val runEnvironment: RunEnvironment,
    val kafkaConfig: Properties,
    val sykmeldingTopic: String,
    val jdbcUrl: String,
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName)
        ?: defaultValue
        ?: throw RuntimeException("Missing required variable \"$varName\"")


fun Application.createEnvironment(): Environment {
    val env = getEnvVar("NAIS_CLUSTER_NAME", "LOCAL")
    val runEnvironment: RunEnvironment = when {
        env.contains("prod") -> RunEnvironment.PROD
        env.contains("dev") -> RunEnvironment.DEV
        else -> RunEnvironment.LOCAL
    }

    return Environment(
        runEnvironment = runEnvironment,
        kafkaConfig = when(runEnvironment) {
            RunEnvironment.LOCAL -> Properties().apply {
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



