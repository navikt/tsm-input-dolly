package no.nav.tsm.sykmelding.repository

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import no.nav.tsm.sykmelding.mapper.mapToSykmeldingRecord
import no.nav.tsm.sykmelding.model.Aktivitet
import no.nav.tsm.sykmelding.model.DollySykmelding
import no.nav.tsm.sykmelding.testcontainers.PostgresSQL.Companion.postgreSQLContainer
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SykmeldingRepositoryTest {


    private lateinit var dataSource: DataSource
    private lateinit var repository: SykmeldingRepository
    private val objectMapper: ObjectMapper = sykmeldingObjectMapper

    @BeforeTest
    fun setup() {
        dataSource = PGSimpleDataSource().apply {
            setURL(postgreSQLContainer.jdbcUrl)
        }

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migrations")
            .load()
        flyway.migrate()


        repository = SykmeldingRepository(dataSource, objectMapper)
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
    fun `should delete sykmelding`() = runTest {
        val sykmeldingId = UUID.randomUUID().toString()
        val ident = "12345678901"
        val dollySykmelding = createTestDollySykmelding(ident)
        val sykmeldingRecord = mapToSykmeldingRecord(sykmeldingId, dollySykmelding)

        repository.saveSykmelding(sykmeldingId, ident, sykmeldingRecord)
        val retrieved = repository.getById(sykmeldingId)

        assertNotNull(retrieved)
        repository.deleteBySykmeldingId(sykmeldingId)
        val deleted = repository.getById(sykmeldingId)
        assertNull(deleted)
    }

    @Test
    fun `should save and retrieve sykmelding by id`() = runTest {
        val sykmeldingId = UUID.randomUUID().toString()
        val ident = "12345678901"
        val dollySykmelding = createTestDollySykmelding(ident)
        val sykmeldingRecord = mapToSykmeldingRecord(sykmeldingId, dollySykmelding)

        repository.saveSykmelding(sykmeldingId, ident, sykmeldingRecord)
        val retrieved = repository.getById(sykmeldingId)

        assertNotNull(retrieved)
        assertEquals(sykmeldingId, retrieved.sykmelding.id)
        assertEquals(ident, retrieved.sykmelding.pasient.fnr)
    }

    @Test
    fun `should return null when sykmelding not found by id`() = runBlocking {
        val retrieved = repository.getById(UUID.randomUUID().toString())

        assertNull(retrieved)
    }

    @Test
    fun `should save and retrieve sykmeldinger by ident`() = runBlocking {
        val ident = "12345678901"
        val sykmeldingId1 = UUID.randomUUID().toString()
        val sykmeldingId2 = UUID.randomUUID().toString()
        val dollySykmelding1 = createTestDollySykmelding(ident)
        val dollySykmelding2 = createTestDollySykmelding(ident)
        val sykmeldingRecord1 = mapToSykmeldingRecord(sykmeldingId1, dollySykmelding1)
        val sykmeldingRecord2 = mapToSykmeldingRecord(sykmeldingId2, dollySykmelding2)

        repository.saveSykmelding(sykmeldingId1, ident, sykmeldingRecord1)
        repository.saveSykmelding(sykmeldingId2, ident, sykmeldingRecord2)
        val retrieved = repository.getByIdent(ident)

        assertEquals(2, retrieved.size)
        val ids = retrieved.map { it.sykmelding.id }.toSet()
        assertEquals(setOf(sykmeldingId1, sykmeldingId2), ids)
    }

    @Test
    fun `should return empty list when no sykmeldinger found by ident`() = runBlocking {
        val retrieved = repository.getByIdent("non-existent-ident")

        assertEquals(0, retrieved.size)
    }

    @Test
    fun `should save complex sykmelding record with all fields`() = runBlocking {
        val sykmeldingId = UUID.randomUUID().toString()
        val ident = "98765432109"
        val dollySykmelding = createTestDollySykmelding(ident)
        val sykmeldingRecord = mapToSykmeldingRecord(sykmeldingId, dollySykmelding)

        repository.saveSykmelding(sykmeldingId, ident, sykmeldingRecord)
        val retrieved = repository.getById(sykmeldingId)

        assertNotNull(retrieved)
        assertEquals(sykmeldingId, retrieved.sykmelding.id)
        assertEquals(ident, retrieved.sykmelding.pasient.fnr)
        assertEquals(RuleType.OK, retrieved.validation.status)
        assertEquals(1, retrieved.sykmelding.aktivitet.size)

        val aktivitet = retrieved.sykmelding.aktivitet.first() as AktivitetIkkeMulig
        assertEquals(LocalDate.of(2025, 9, 10), aktivitet.fom)
        assertEquals(LocalDate.of(2025, 9, 20), aktivitet.tom)
    }

    private fun createTestDollySykmelding(ident: String): DollySykmelding {
        return DollySykmelding(
            ident = ident,
            aktivitet = listOf(Aktivitet(
                fom = LocalDate.of(2025, 9, 10),
                tom = LocalDate.of(2025, 9, 20)
            ))
        )
    }
}
