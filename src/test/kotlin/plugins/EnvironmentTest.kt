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
                    ) + pgEnvs("") + pgEnvs("READWRITE_")
                )
                    .withFallback(ConfigFactory.parseResources("application.conf"))
                    .resolve()
            )

        val environment = createEnvironment(applicationConfig)

        environment.consumer.poll shouldEqual 10.seconds
        environment.consumer.retry shouldEqual 60.seconds
        environment.migrationJdbcUrl shouldEqual expectedUrl("")
        environment.appJdbcUrl shouldEqual expectedUrl("READWRITE_")
    }

    private fun pgEnvs(prefix: String) = mapOf(
        "${prefix}PGHOST" to "${prefix}host",
        "${prefix}PGPORT" to "5432",
        "${prefix}PGDATABASE" to "db",
        "${prefix}PGUSER" to "${prefix}user",
        "${prefix}PGSSLMODE" to "verify-full",
        "${prefix}PGSSLCERT" to "/certs/${prefix}cert.pem",
        "${prefix}PGSSLKEY" to "/certs/${prefix}key.pk8",
        "${prefix}PGSSLROOTCERT" to "/certs/${prefix}root.pem",
        "${prefix}PGJDBCURL" to expectedUrl(prefix)
    )

    private fun expectedUrl(prefix: String) =
        "jdbc:postgresql://${prefix}host:5432/db?user=${prefix}user&sslmode=verify-full" +
            "&sslcert=/certs/${prefix}cert.pem&sslkey=/certs/${prefix}key.pk8&sslrootcert=/certs/${prefix}root.pem"
}
