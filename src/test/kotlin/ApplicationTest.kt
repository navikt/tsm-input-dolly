package no.nav.tsm

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import no.nav.tsm.sykmelding.testcontainers.PostgresSQL.Companion.postgreSQLContainer
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        environment {
            config = MapApplicationConfig(
                "database.url" to postgreSQLContainer.jdbcUrl,
                "database.postgres.url" to postgreSQLContainer.jdbcUrl,
            )
        }
        application {
            module()
        }
        client.get("/internal/is_alive").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

}


