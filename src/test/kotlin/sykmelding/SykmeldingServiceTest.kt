package no.nav.tsm.sykmelding

import kotlinx.coroutines.runBlocking
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import no.nav.tsm.sykmelding.model.Aktivitet
import no.nav.tsm.sykmelding.model.DollySykmelding
import no.nav.tsm.sykmelding.repository.SykmeldingRepository
import no.nav.tsm.sykmelding.testcontainers.PostgresSQL.Companion.postgreSQLContainer
import org.flywaydb.core.Flyway
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.postgresql.ds.PGSimpleDataSource
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class SykmeldingServiceTest {

    private lateinit var dataSource: DataSource
    private lateinit var repository: SykmeldingRepository
    private lateinit var mockKafkaProducer: SykmeldingInputProducer
    private lateinit var service: SykmeldingService

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

        repository = SykmeldingRepository(dataSource, sykmeldingObjectMapper)
        mockKafkaProducer = mock<SykmeldingInputProducer>()
        service = SykmeldingService(mockKafkaProducer, repository)
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
            aktivitet = Aktivitet(
                fom = LocalDate.of(2025, 9, 10),
                tom = LocalDate.of(2025, 9, 20)
            )
        )

        val sykmeldingId = service.opprettSykmelding(dollySykmelding)

        assertNotNull(sykmeldingId)
        UUID.fromString(sykmeldingId)

        val savedSykmelding = repository.getById(sykmeldingId)
        assertNotNull(savedSykmelding)

        verify(mockKafkaProducer).sendSykmelding(org.mockito.kotlin.any<SykmeldingRecord>())
    }
}
