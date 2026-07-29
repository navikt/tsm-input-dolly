package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.ktor.logger
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource

fun Application.configureDatabase() {
    val logger = logger()
    val env: Environment by dependencies
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



