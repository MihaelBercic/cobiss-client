package cobiss

import cobiss.builder.project.Projects
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
class CobissClient(private val username: String, private val password: String, private val system: String, private val language: Language) {

    private val json = Json { ignoreUnknownKeys = true }
    private val url = "https://cris.cobiss.net/$system/${language.abbreviation}/service"
    private val httpClient = HttpClient.newHttpClient()
    private var token: String = fetchToken()

    @Serializable
    private data class JWTRequest(val username: String, val password: String)

    @Serializable
    private data class JWTResponse(val jwt: String)

    private fun fetchToken(): String {
        val encodedCredentials = json.encodeToString(JWTRequest(username, password))
        val tokenRequest = HttpRequest.newBuilder(URI("$url/getjwt")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(encodedCredentials)).build()
        val response = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString())
        val responseBody = response.body()
        val serialized = json.decodeFromString<JWTResponse>(responseBody)
        return serialized.jwt
    }

    fun fetch(endpoint: String, requestBuilder: HttpRequest.Builder = HttpRequest.newBuilder()): HttpResponse<String> {
        val finishedRequest = requestBuilder.uri(URI("$url/$endpoint")).header("Content-Type", "application/json").header("Authorization", token).build()
        return httpClient.send(finishedRequest, HttpResponse.BodyHandlers.ofString())
    }

    val projects get() = Projects(this)

}
