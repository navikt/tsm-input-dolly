package no.nav.tsm.texas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.tsm.ktor.logger
import no.nav.tsm.plugins.Environment

data class TexasResponse(
    val access_token: String,
    val expires_in: Int,
    val token_type: String,
)

data class TexasRequest(
    val identity_provider: String,
    val target: String,
)
class TexasClient(
    private val httpClient: HttpClient,
    environment: Environment
) {
    val log = logger()
    val tokenEndpoint = environment.texasUrl

    suspend fun getAccessToken(scope: String): String {

        val requestBody = TexasRequest(
            identity_provider = "azuread",
            target = scope,
        )

        val response = httpClient.post(tokenEndpoint) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        log.info("Texas endpoint returned ${response.status}")
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Token endpoint returned ${response.status}")
        } else {
            return response.body<TexasResponse>().access_token
        }
    }
}
