package no.nav.tsm.sykmelding.testcontainers

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
class PostgresSQL {
    companion object {
        @Container
        val postgreSQLContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:17")
            .withDatabaseName("app")
            .withExposedPorts(5432)
            .withUsername("user")
            .withPassword("pw")
            .withUrlParam("user", "user")
            .withUrlParam("password", "pw")
            .waitingFor(Wait.forListeningPort())
            .apply {
                start()
            }
    }
}
