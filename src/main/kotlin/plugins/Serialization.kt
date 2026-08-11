package no.nav.tsm.plugins

import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {}
    }
}
