package no.nav.tsm.plugins

import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson3.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.ktor.auth.texas.Texas
import no.nav.tsm.ktor.kafka.sykmeldinger.SykmeldingInputProducer
import no.nav.tsm.ktor.kafka.sykmeldinger.sykmeldingInputProducer
import no.nav.tsm.pdl.TsmPdlClient
import no.nav.tsm.sykmelding.SykmeldingService
import no.nav.tsm.sykmelding.consumer.SykmeldingConsumerService
import no.nav.tsm.sykmelding.repository.SykmeldingRepository
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

fun Application.configureDependencies() {
    dependencies {
        provide<Environment> { createEnvironment(this@configureDependencies.environment.config) }
        provide<HttpClient> { configureBaseHttpClient() }
        provide(Texas::class)
        provide(TsmPdlClient::class)
        provide<DataSource> {
            PGSimpleDataSource().apply { setURL(resolve<Environment>().jdbcUrl) }
        }
        provide<SykmeldingInputProducer> { this@configureDependencies.sykmeldingInputProducer() }
        provide(SykmeldingRepository::class)
        provide(SykmeldingConsumerService::class)
        provide(SykmeldingService::class)

    }
}

private fun configureBaseHttpClient(): HttpClient = HttpClient(Apache5) {
    install(ContentNegotiation) {
        jackson {}
    }
}
