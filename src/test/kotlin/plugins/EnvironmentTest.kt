package no.nav.tsm.plugins

import com.typesafe.config.ConfigFactory
import io.kotest.matchers.equals.shouldEqual
import io.ktor.server.config.*
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class EnvironmentTest {
    @Test
    fun `production environment should be properly configured, even lazy values`() {
        val applicationConfig =
            HoconApplicationConfig(
                ConfigFactory.parseMap(
                    mapOf(
                        // Nais injected values
                        "NAIS_POD_NAME" to "syk-inn-api-prod-123",
                        "NAIS_CLUSTER_NAME" to "prod-gcp",
                    )
                )
                    .withFallback(ConfigFactory.parseResources("application.conf"))
                    .resolve()
            )

        val environment = createEnvironment(applicationConfig)

        environment.consumer.poll shouldEqual 10.seconds
        environment.consumer.retry shouldEqual 60.seconds
    }
}
