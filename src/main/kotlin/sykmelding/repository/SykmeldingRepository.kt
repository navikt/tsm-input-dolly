package no.nav.tsm.sykmelding.repository

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class SykmeldingRepository(private val dataSource: DataSource, private val objectMapper: ObjectMapper) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun saveSykmelding(sykmeldingId: String, ident: String, sykmeldingRecord: SykmeldingRecord) = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO sykmelding (sykmeldingId, ident, sykmelding) 
            VALUES (?, ?, ?::jsonb)
            ON CONFLICT (sykmeldingId) 
            DO UPDATE SET 
                ident = EXCLUDED.ident,
                sykmelding = EXCLUDED.sykmelding
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, sykmeldingId)
                statement.setString(2, ident)
                statement.setString(3, objectMapper.writeValueAsString(sykmeldingRecord))
                statement.executeUpdate()
            }
        }
    }

    suspend fun getByIdent(ident: String): List<SykmeldingRecord> = withContext(Dispatchers.IO) {
        val sql = "SELECT sykmeldingId, ident, sykmelding FROM sykmelding WHERE ident = ?"
        val sykmeldinger = mutableListOf<SykmeldingRecord>()
        val incorrectSykmeldingerIds = mutableListOf<String>()
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, ident)
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        try {
                            val json = resultSet.getString("sykmelding")
                            val sykmeldingRecord = objectMapper.readValue(json, SykmeldingRecord::class.java)
                            sykmeldinger.add(sykmeldingRecord)
                        } catch (ex: JacksonException) {
                            val id = resultSet.getString("sykmeldingId")
                            logger.warn("Error while fetching sykmelding from db with $id")
                            incorrectSykmeldingerIds.add(id)
                        }
                    }
                }
            }
        }
        incorrectSykmeldingerIds.forEach {
            val sykmeldingId = deleteBySykmeldingId(it)
            logger.info("Deleted sykmelding with id $sykmeldingId")
        }
        sykmeldinger
    }

    suspend fun getById(sykmeldingId: String): SykmeldingRecord? = withContext(Dispatchers.IO) {
        val sql = "SELECT sykmelding FROM sykmelding WHERE sykmeldingId = ?"

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, sykmeldingId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        val json = resultSet.getString("sykmelding")
                        objectMapper.readValue(json, SykmeldingRecord::class.java)
                    } else {
                        null
                    }
                }
            }
        }
    }

    suspend fun deleteBySykmeldingId(sykmeldingId: String) = withContext(Dispatchers.IO) {
        val sql = "DELETE FROM sykmelding WHERE sykmeldingId = ?"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, sykmeldingId)
                statement.executeUpdate()
            }
        }
    }
    suspend fun deleteByIdent(ident: String)  = withContext(Dispatchers.IO) {
        val sql = "DELETE FROM sykmelding WHERE ident = ?"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, ident)
                statement.executeUpdate()
            }
        }
    }

    suspend fun saveCorruptData(sykmeldingId: String, data: ByteArray) = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO corrupt_data(sykmeldingId, data) 
            VALUES (?, ?)
            ON CONFLICT (sykmeldingId) 
            DO UPDATE SET 
                data = EXCLUDED.data
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, sykmeldingId)
                statement.setBytes(2, data)
                statement.executeUpdate()
            }
        }
    }
}
