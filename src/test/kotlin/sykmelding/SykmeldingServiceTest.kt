package no.nav.tsm.sykmelding

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.tsm.sykmelding.exceptions.SykmeldingValidationException
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import no.nav.tsm.sykmelding.model.Aktivitet
import no.nav.tsm.sykmelding.model.DollySykmelding
import no.nav.tsm.sykmelding.repository.SykmeldingRepository
import no.nav.tsm.sykmelding.testcontainers.PostgresSQL.Companion.postgreSQLContainer
import no.nav.tsm.`tsm-pdl`.Navn
import no.nav.tsm.`tsm-pdl`.TsmPdlClient
import no.nav.tsm.`tsm-pdl`.TsmPdlResponse
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.postgresql.ds.PGSimpleDataSource
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SykmeldingServiceTest {

    private lateinit var dataSource: DataSource
    private lateinit var repository: SykmeldingRepository
    private lateinit var mockKafkaProducer: SykmeldingInputProducer
    private lateinit var service: SykmeldingService
    private val tsmPdlClient = mockk<TsmPdlClient>(relaxed = true)

    @BeforeTest
    fun setup() {
        dataSource = PGSimpleDataSource().apply {
            setURL(postgreSQLContainer.jdbcUrl)
            setUser(postgreSQLContainer.username)
            setPassword(postgreSQLContainer.password)
        }

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migrations")
            .load()
        flyway.migrate()
        coEvery { tsmPdlClient.getPerson(any()) } returns TsmPdlResponse(
            false, navn = Navn("fornavn", null, "etternavn"), fodselsdato = LocalDate.now(), doed = false
        )
        repository = SykmeldingRepository(dataSource, sykmeldingObjectMapper)
        mockKafkaProducer = mock<SykmeldingInputProducer>()
        service = SykmeldingService(mockKafkaProducer, repository, tsmPdlClient)
    }

    @AfterTest
    fun cleanup() {
        dataSource.connection.use { connection ->
            connection.prepareStatement("DELETE FROM sykmelding").use { statement ->
                statement.executeUpdate()
            }
        }
    }

    @Test
    fun `should save sykmelding to database and send to kafka`() = runBlocking {
        val dollySykmelding = DollySykmelding(
            ident = "12345678901",
            aktivitet = listOf(Aktivitet(
                fom = LocalDate.of(2025, 9, 10),
                tom = LocalDate.of(2025, 9, 20)
            ))
        )

        val sykmeldingId = service.opprettSykmelding(dollySykmelding)

        assertNotNull(sykmeldingId)
        UUID.fromString(sykmeldingId)

        val savedSykmelding = repository.getById(sykmeldingId)
        assertNotNull(savedSykmelding)

        verify(mockKafkaProducer).sendSykmelding(org.mockito.kotlin.any<SykmeldingRecord>())
    }

    @Test
    fun `validate sykmelding grad is not in range`() = runTest {
        val dollySykmelding = DollySykmelding(
            ident = "12345678901",
            aktivitet = listOf(Aktivitet(
                fom = LocalDate.of(2025, 9, 10),
                tom = LocalDate.of(2025, 9, 20),
                grad = 101
            ))
        )

        assertThrows<SykmeldingValidationException>("Grad must be in rage 1-99") { service.opprettSykmelding(dollySykmelding) }
    }

    @Test
    fun `validate fnr is not 11 digits`() = runTest {
        val dollySykmelding = DollySykmelding(
            ident = "123456789011",
            aktivitet = listOf(Aktivitet(
                fom = LocalDate.of(2025, 9, 10),
                tom = LocalDate.of(2025, 9, 20),
                grad = 101
            ))
        )

        assertThrows<SykmeldingValidationException>("fnr må vere 11 siffer. Lengde: ${"12345678901".length}") { service.opprettSykmelding(dollySykmelding) }

    }

    @Test
    fun `Person not in PDL`() = runTest {
        val dollySykmelding = DollySykmelding(
            ident = "12345678901",
            aktivitet = listOf(Aktivitet(
                fom = LocalDate.of(2025, 9, 10),
                tom = LocalDate.of(2025, 9, 20),
                grad = 101
            ))
        )
        coEvery { tsmPdlClient.getPerson(any()) } returns null
        assertThrows<SykmeldingValidationException>("Fant ikke person i PDL") { service.opprettSykmelding(dollySykmelding) }

    }

    @Test
    fun `Aktivitet fom after tom`() = runTest {
        val aktivitet = Aktivitet(
            fom = LocalDate.of(2025, 9, 22),
            tom = LocalDate.of(2025, 9, 20),
        )
        val dollySykmelding = DollySykmelding(
            ident = "12345678901",
            aktivitet = listOf(aktivitet)
        )

        assertThrows<SykmeldingValidationException>("Fom ${aktivitet.fom} must be equal or before ${aktivitet.tom}") { service.opprettSykmelding(dollySykmelding) }

    }
    @Test
    fun `Aktivitet fom is not after previous aktivitet tom`() = runTest {
        val first = Aktivitet(
            fom = LocalDate.of(2025, 9, 10),
            tom = LocalDate.of(2025, 9, 20),
        )
        val second = Aktivitet(
            fom = LocalDate.of(2025, 9, 20),
            tom = LocalDate.of(2025, 9, 23),
        )
        val dollySykmelding = DollySykmelding(
            ident = "12345678901",
            aktivitet = listOf(first, second)
        )

        val exception = assertThrows<SykmeldingValidationException>() { service.opprettSykmelding(dollySykmelding) }
        assertEquals("Fom: ${second.fom} in aktivitet must be after tom ${first.tom} in previous aktivitet",
            exception.message)
    }

    @Test
    fun `No gap in aktivitet if first tom is monday-thursday`() = runTest {
        val first = Aktivitet(
            fom = LocalDate.of(2025, 9, 1), //Monday
            tom = LocalDate.of(2025, 9, 2), //Tuesday
        )
        val second = Aktivitet(
            fom = LocalDate.of(2025, 9, 3), //Wednesday
            tom = LocalDate.of(2025, 9, 4), //Thursday
        )
        val dollySykmelding = DollySykmelding(
            ident = "12345678901",
            aktivitet = listOf(first, second)
        )

        assertDoesNotThrow { service.opprettSykmelding(dollySykmelding) }

    }

    @Test
    fun `throw if gap in aktivitet first tom is monday-thursday`() = runTest {
        val first = Aktivitet(
            fom = LocalDate.of(2025, 9, 1), //Monday
            tom = LocalDate.of(2025, 9, 2), //Tuesday
        )
        val second = Aktivitet(
            fom = LocalDate.of(2025, 9, 4), //Thursday
            tom = LocalDate.of(2025, 9, 5), //Friday
        )
        val dollySykmelding = DollySykmelding(
            ident = "12345678901",
            aktivitet = listOf(first, second)
        )

        val exception = assertThrows<SykmeldingValidationException>() { service.opprettSykmelding(dollySykmelding) }
        assertEquals("Fom: ${second.fom} in aktivitet can not be after ${first.tom.plusDays(1)}",
            exception.message)

    }
    @Test
    fun `ok when gap in aktivitets is 3 when first tom is on friday `() = runTest {
        val first = Aktivitet(
            fom = LocalDate.of(2025, 9, 1), //Monday
            tom = LocalDate.of(2025, 9, 5), //Friday
        )
        val second = Aktivitet(
            fom = LocalDate.of(2025, 9, 8), //Monday
            tom = LocalDate.of(2025, 9, 20),
        )
        val dollySykmelding = DollySykmelding(
            ident = "12345678901",
            aktivitet = listOf(first, second)
        )

        assertDoesNotThrow { service.opprettSykmelding(dollySykmelding) }
    }

    @Test
    fun `Throw when gap in aktivitets is more than 3 when first tom is on friday `() = runTest {
        val first = Aktivitet(
            fom = LocalDate.of(2025, 9, 1), //Monday
            tom = LocalDate.of(2025, 9, 5), //Friday
        )
        val second = Aktivitet(
            fom = LocalDate.of(2025, 9, 9), //Monday
            tom = LocalDate.of(2025, 9, 20),
        )
        val dollySykmelding = DollySykmelding(
            ident = "12345678901",
            aktivitet = listOf(first, second)
        )
        val exception = assertThrows<SykmeldingValidationException>() { service.opprettSykmelding(dollySykmelding) }
        assertEquals("Fom: ${second.fom} in aktivitet can not be after ${first.tom.plusDays(3)}",
            exception.message)
    }
}
