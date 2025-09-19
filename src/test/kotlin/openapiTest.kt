package no.nav.tsm

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.mockk.coEvery
import io.mockk.mockk
import io.restassured.RestAssured
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.tsm.sykmelding.SykmeldingService
import no.nav.tsm.sykmelding.api.sykmeldingApi
import no.nav.tsm.sykmelding.exceptions.SykmeldingValidationException
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import no.nav.tsm.sykmelding.model.Aktivitet
import no.nav.tsm.sykmelding.model.DollySykmelding
import no.nav.tsm.sykmelding.model.DollySykmeldingResponse
import no.nav.tsm.sykmelding.model.DollySykmeldingerResponse
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import java.time.LocalDate
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenApiTest {

        private val sykmeldingService = mockk<SykmeldingService>()
        val scope = CoroutineScope(Dispatchers.IO)
        private val filter: OpenApiValidationFilter
        private val application = scope.
            embeddedServer(factory = Netty, port = 0) {
                configureRouting()
                configureSerialization()
                install(Koin) {
                    slf4jLogger()
                    modules(module {
                        single {
                            sykmeldingService
                        }
                    })
                }
                routing {
                    route("/api") {
                        sykmeldingApi()
                    }
                }
            }.start(wait = false)

        val thisPort = runBlocking { application.engine.resolvedConnectors().first().port }

        init {
            RestAssured.baseURI = "http://localhost"
            RestAssured.port = thisPort
            RestAssured.defaultParser
            val oas = "src/main/resources/openapi/documentation.yaml"
            filter = OpenApiValidationFilter(oas)
        }

        @AfterAll
        fun afterAll() {
            application.stop()
        }

        @Test
        fun `Test get sykmelidng 404`() {

            coEvery { sykmeldingService.hentSykmelding(any()) } returns null

            RestAssured
                .given()
                .filter(filter)
                .accept("application/json")
                .`when`()
                .get("/api/sykmelding/123")
                .then()
                .statusCode(404)

        }

        @Test
        fun `Test GET sykmelding InternalServerError`() {
            coEvery { sykmeldingService.hentSykmelding(any()) } throws RuntimeException("Test exception")

            RestAssured
                .given()
                .filter(filter)
                .accept("application/json")
                .`when`()
                .get("/api/sykmelding/123")
                .then()
                .statusCode(500)
                .body(
                    "message",
                    equalTo("Error while getting sykmelding with id: 123"),
                )
        }

        @Test
        fun `Test get sykmelidng 200`() {
            val dollySykmeldingResponse = DollySykmeldingResponse(
                ident = "12345678912",
                sykmeldingId = "id",
                aktivitet = listOf(Aktivitet(
                    fom = LocalDate.of(2025, 9, 10),
                    tom = LocalDate.of(2025, 9, 20),
                ))
            )
            coEvery { sykmeldingService.hentSykmelding(any()) } returns dollySykmeldingResponse

            val result = RestAssured
                .given()
                .filter(filter)
                .get("/api/sykmelding/123")
                .then()
                .statusCode(200)
                .extract().asString().let {
                    sykmeldingObjectMapper.readValue(it, DollySykmeldingResponse::class.java)
                }
            Assert.assertEquals(dollySykmeldingResponse, result)
        }

    @Test
    fun `Test GET sykmeldinger for ident`() {
        val ident = "12345678912"
        val dollySykmeldingerResponse = DollySykmeldingerResponse(
            listOf(
                DollySykmeldingResponse(
                    ident = ident,
                    sykmeldingId = "id",
                    aktivitet = listOf(Aktivitet(
                        fom = LocalDate.of(2025, 9, 10),
                        tom = LocalDate.of(2025, 9, 20)
                    ))
                )
            )
        )
        coEvery { sykmeldingService.hentSykmeldingByIdent(any()) } returns dollySykmeldingerResponse

        val result = RestAssured
            .given()
            .filter(filter)
            .header("X-ident", ident)
            .get("/api/sykmelding/ident")
            .then()
            .statusCode(200)
            .extract().asString().let {
                sykmeldingObjectMapper.readValue(it, DollySykmeldingerResponse::class.java)
            }
        Assert.assertEquals(dollySykmeldingerResponse, result)

    }

    @Test
    fun `Test GET sykmeldinger for ident 500 response`() {
        val ident = "12345678912"

        coEvery { sykmeldingService.hentSykmeldingByIdent(any()) } throws RuntimeException("Test exception")

        RestAssured
            .given()
            .filter(filter)
            .header("X-ident", ident)
            .get("/api/sykmelding/ident")
            .then()
            .statusCode(500)

    }

    @Test
    fun `Post gives 400 when person not in folkeregisteret`() {

        coEvery { sykmeldingService.opprettSykmelding(any()) } throws(SykmeldingValidationException("person not found in pdl"))
        val dollySykmelding = DollySykmelding(
            ident = "12345678912",
            aktivitet = listOf(Aktivitet(
                fom = LocalDate.of(2025, 9, 10),
                tom = LocalDate.of(2025, 9, 20)
            ))
        )
        RestAssured
            .given()
            .filter(filter)
            .contentType("application/json")
            .body(sykmeldingObjectMapper.writeValueAsString(dollySykmelding))
            .post("/api/sykmelding")
            .then()
            .statusCode(400)
    }

    @Test
    fun `Post gives 500 when some other error happens`() {
        coEvery { sykmeldingService.opprettSykmelding(any()) } throws(RuntimeException("Exception"))


        val dollySykmelding = DollySykmelding(
            ident = "12345678912",
            aktivitet = listOf(Aktivitet(
                fom = LocalDate.of(2025, 9, 10),
                tom = LocalDate.of(2025, 9, 20)
            ))
        )
        RestAssured
            .given()
            .filter(filter)
            .contentType("application/json")
            .body(sykmeldingObjectMapper.writeValueAsString(dollySykmelding))
            .post("/api/sykmelding")
            .then()
            .statusCode(500)
    }

    @Test
    fun `Test POST sykmelding 200`() {
        val sykmeldingId = "123"
        coEvery { sykmeldingService.opprettSykmelding(any()) } returns sykmeldingId
        val dollySykmelding = DollySykmelding(
            ident = "12345678912",
            aktivitet = listOf(Aktivitet(
                fom = LocalDate.of(2025, 9, 10),
                tom = LocalDate.of(2025, 9, 20)
            ))
        )

        val dollySykmeldingResponse = DollySykmeldingResponse(
            ident = dollySykmelding.ident,
            aktivitet = dollySykmelding.aktivitet,
            sykmeldingId = sykmeldingId
        )


        val result = RestAssured
            .given()
            .filter(filter)
            .contentType("application/json")
            .body(sykmeldingObjectMapper.writeValueAsString(dollySykmelding))
            .post("/api/sykmelding")
            .then()
            .statusCode(200)
            .extract().asString().let {
            sykmeldingObjectMapper.readValue(it, DollySykmeldingResponse::class.java)
        }
        Assert.assertEquals(dollySykmeldingResponse, result)

    }


    @Test
    fun `Test POST sykmelding 200 with grad`() {
        val sykmeldingId = "123"
        coEvery { sykmeldingService.opprettSykmelding(any()) } returns sykmeldingId
        val dollySykmelding = DollySykmelding(
            ident = "12345678912",
            aktivitet = listOf(Aktivitet(
                fom = LocalDate.of(2025, 9, 10),
                tom = LocalDate.of(2025, 9, 20),
                grad = 50
            ))
        )

        RestAssured
            .given()
            .filter(filter)
            .contentType("application/json")
            .body(sykmeldingObjectMapper.writeValueAsString(dollySykmelding))
            .post("/api/sykmelding")
            .then()
            .statusCode(200)

    }
    @Test
    fun `Test POST sykmelding with invalid grad gives 400`() {
        coEvery { sykmeldingService.opprettSykmelding(any()) } throws(SykmeldingValidationException("Grad must be in rage 1-99"))

        val aktivitet = Aktivitet(
            fom = LocalDate.of(2025, 9, 10),
            tom = LocalDate.of(2025, 9, 20),
            grad = 0
        )
        val dollySykmelding = DollySykmelding(
            ident = "12345678912",
            aktivitet = listOf(aktivitet)
        )


        RestAssured
            .given()
            .contentType("application/json")
            .body(sykmeldingObjectMapper.writeValueAsString(dollySykmelding))
            .post("/api/sykmelding")
            .then()
            .statusCode(400)
        RestAssured
            .given()
            .contentType("application/json")
            .body(sykmeldingObjectMapper.writeValueAsString(dollySykmelding.copy(aktivitet = listOf(aktivitet.copy(grad = 100)))))
            .post("/api/sykmelding")
            .then()
            .statusCode(400)

    }

    @Test
    fun `Test POST sykmelding with invalid aktivitet gives 400`() {
        coEvery { sykmeldingService.opprettSykmelding(any()) } throws(SykmeldingValidationException("person not found in pdl"))
        val aktivitet = Aktivitet(
            fom = LocalDate.of(2025, 9, 10),
            tom = LocalDate.of(2025, 9, 20),
            grad = 0
        )
        val dollySykmelding = DollySykmelding(
            ident = "12345678912",
            aktivitet = listOf(aktivitet)
        )

        RestAssured
            .given()
            .contentType("application/json")
            .body(sykmeldingObjectMapper.writeValueAsString(dollySykmelding))
            .post("/api/sykmelding")
            .then()
            .statusCode(400)
    }

    @Test
    fun `delete sykmelding`() {
        val ident = "12345678912"
        coEvery { sykmeldingService.deleteSykmeldingerForIdent(any()) } returns Unit
        RestAssured
            .given()
            .filter(filter)
            .header("X-ident", ident)
            .delete("/api/sykmelding/ident")
            .then()
            .statusCode(200)
    }

    @Test
    fun `delete sykmelding 500 error`() {
        val ident = "12345678912"
        coEvery { sykmeldingService.deleteSykmeldingerForIdent(any()) } throws RuntimeException("Test exception")
        RestAssured
            .given()
            .filter(filter)
            .header("X-ident", ident)
            .delete("/api/sykmelding/ident")
            .then()
            .statusCode(500)
    }
}

