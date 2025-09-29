package no.nav.tsm.`tsm-pdl`

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import no.nav.tsm.texas.TexasClient
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


class TsmPdlClient(private val texasClient: TexasClient,
                    private val httpClient: HttpClient,
                   private val tsmScope: String,
                   private val tsmUrl: String) {

    suspend fun getPerson(ident: String) : TsmPdlResponse? {
        val token = texasClient.getAccessToken(tsmScope)
        val response = httpClient.get("$tsmUrl/api/person") {
            bearerAuth(token)
            header("ident", ident)
            accept(ContentType.Application.Json)
        }
        if(response.status == HttpStatusCode.OK) {
            val body = response.body<TsmPdlResponse>()
            return body
        }
        return null
    }

}
