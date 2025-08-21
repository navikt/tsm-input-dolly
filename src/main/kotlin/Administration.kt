package no.nav.tsm

import io.ktor.server.application.*
import io.ktor.server.engine.*

fun Application.configureAdministration() {
    install(ShutDownUrl.ApplicationCallPlugin) {
        exitCodeSupplier = { 0 }
    }
}
