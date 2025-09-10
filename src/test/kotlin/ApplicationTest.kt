package no.nav.tsm

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        client.get("/internal/is_alive").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

}
