package cobiss

import cobiss.builder.CobissQuery
import cobiss.builder.project.ProjectsQueryBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse

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
        val tokenRequest = HttpRequest.newBuilder(URI("$url/getjwt"))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(encodedCredentials))
            .build()
        val response = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString())
        val responseBody = response.body()
        val serialized = json.decodeFromString<JWTResponse>(responseBody)
        return serialized.jwt
    }

    fun fetch(query: CobissQuery): CobissQueryResponse {
        val queryUrl = "${query.endpoint}/${query.queryUrl()}"
        val request = HttpRequest.newBuilder(URI("$url/$queryUrl"))
            .header("Content-Type", "application/json")
            .header("Authorization", token)
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return CobissQueryResponse(response.body())
    }

    fun projects(): ProjectsQueryBuilder = ProjectsQueryBuilder()
}

data class CobissQueryResponse(val body: String) {
    inline fun <reified T> parse() = Json.decodeFromString<T>(body)
}





