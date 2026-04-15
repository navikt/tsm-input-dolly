package no.nav.tsm.sykmelding.testcontainers

import net.bytebuddy.utility.dispatcher.JavaDispatcher
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.postgresql.PostgreSQLContainer


class PostgresSQL {

    companion object {
        val postgres = PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("app")
            .withExposedPorts(5432)
            .withUsername("user")
            .withPassword("pw")
            .withUrlParam("user", "user")
            .withUrlParam("password", "pw")
            .apply { start() }
    }
}
