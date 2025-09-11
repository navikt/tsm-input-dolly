package no.nav.tsm

import io.ktor.server.application.*
import no.nav.tsm.plugins.Environment
import org.flywaydb.core.Flyway
import org.koin.ktor.ext.get
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DatabaseConfig")

fun Application.configureDatabase() {
    val env: Environment = get()
    val url = env.jdbcUrl

    logger.info("Running Flyway migrations...")

    val flyway = Flyway.configure()
        .dataSource(PGSimpleDataSource().apply {
            setURL(url)
        })
        .locations("db/migrations")
        .load()

    flyway.migrate()

    logger.info("Flyway migrations completed successfully")
}



