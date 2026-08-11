package no.nav.tsm.pdl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import no.nav.tsm.ktor.auth.texas.Texas
import no.nav.tsm.ktor.auth.texas.TexasToken
import java.time.LocalDate

data class TsmPdlResponse(
    val falskIdent: Boolean,
    val navn: Navn?,
    val fodselsdato: LocalDate?,
    val doed: Boolean,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)


class TsmPdlClient(
    private val texasClient: Texas,
    private val httpClient: HttpClient,
) {

    private val tsmPdlCacheUrl = "http://tsm-pdl-cache"

    suspend fun getPerson(ident: String): TsmPdlResponse? {
        val (token) = this.getToken()
        val response = httpClient.get("$tsmPdlCacheUrl/api/person") {
            bearerAuth(token)
            header("ident", ident)
            accept(ContentType.Application.Json)
        }
        if (response.status == HttpStatusCode.OK) {
            val body = response.body<TsmPdlResponse>()
            return body
        }
        return null
    }

    private suspend fun getToken(): TexasToken = texasClient.entraIdToken("tsm", "tsm-pdl-cache")
}
