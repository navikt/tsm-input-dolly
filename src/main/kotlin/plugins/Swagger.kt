package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.*

fun Application.configureSwagger() {
    routing {
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
        swaggerUI(path = "swagger-ui", swaggerFile = "openapi/documentation.yaml")
    }
}
