package no.nav.tsm.plugins

import io.ktor.server.config.*
import io.ktor.server.application.*
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.nais.getRuntimeCluster
import kotlin.time.Duration

class Runtime(val env: RuntimeCluster, val name: String)

class ConsumerConfig(val poll: Duration, val retry: Duration)

class Environment(
    val runtime: Runtime,
    val jdbcUrl: String,
    val consumer: ConsumerConfig,
)

fun createEnvironment(config: ApplicationConfig): Environment {
    return Environment(
        runtime = Runtime(
            env = getRuntimeCluster(),
            name = config.property("app.name").getString()
        ),
        consumer = ConsumerConfig(
            poll = config.property("consumer.poll").getAs(),
            retry = config.property("consumer.retry").getAs(),
        ),
        jdbcUrl = config.property("database.url").getString(),
    )
}
