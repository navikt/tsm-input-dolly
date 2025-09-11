package no.nav.tsm.sykmelding.repository

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import javax.sql.DataSource

class SykmeldingRepository(private val dataSource: DataSource, private val objectMapper: ObjectMapper) {

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

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, ident)
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val json = resultSet.getString("sykmelding")
                        val sykmeldingRecord = objectMapper.readValue(json, SykmeldingRecord::class.java)
                        sykmeldinger.add(sykmeldingRecord)
                    }
                }
            }
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
