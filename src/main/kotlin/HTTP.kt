package no.nav.tsm

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.routing.*

fun Application.configureHTTP() {
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
    }

    routing {
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
    }
}
