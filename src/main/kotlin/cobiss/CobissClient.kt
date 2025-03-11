package cobiss

import cobiss.builder.organisation.OrganisationAPI
import cobiss.builder.project.ProjectsAPI
import cobiss.builder.researcher.ResearchersAPI
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logging.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
class CobissClient(private val username: String, private val password: String, private val system: String, private val language: Language) {

    val json = Json { ignoreUnknownKeys = true }
    private val url = "https://cris.cobiss.net/$system/${language.abbreviation}/service"
    private val httpClient = HttpClient.newHttpClient()
    private var token: String = fetchToken()
    private var lastTokenAccess: Long = 0L

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
        if (System.currentTimeMillis() - lastTokenAccess >= 20 * 60 * 60 * 1000) {
            token = fetchToken()
            lastTokenAccess = System.currentTimeMillis()
        }
        val finishedRequest = requestBuilder.uri(URI("$url/$endpoint").apply(Logger::info))
            .header("Content-Type", "application/json")
            .header("Authorization", token)
            .build()
        return httpClient.send(finishedRequest, HttpResponse.BodyHandlers.ofString())
    }

    val projects get() = ProjectsAPI(this)
    val researchers get() = ResearchersAPI(this)
    val organisations get() = OrganisationAPI(this)

    // 36213 kikiriki
}
