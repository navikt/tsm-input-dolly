package no.nav.tsm

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.tsm.ktor.auth.texas.TexasClient
import no.nav.tsm.sykmelding.testcontainers.PostgresSQL.Companion.postgres
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        environment {
            config = MapApplicationConfig(
                "database.url" to postgres.jdbcUrl,
            )
        }
        application {
            dependencies {
                provide<TexasClient> { mockk() }
            }
            module()
        }

        client.get("/internal/health/alive").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

}


