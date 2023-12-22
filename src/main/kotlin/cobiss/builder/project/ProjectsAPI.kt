package cobiss.builder.project

import CobissAPI
import cobiss.CobissClient
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.net.http.HttpRequest

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
class ProjectsAPI(override val cobissClient: CobissClient) : CobissAPI<ProjectDetails, ProjectsQueryBuilder> {

    override val endpoint: String = "project"

    override fun newQuery() = ProjectsQueryBuilder(endpoint, cobissClient)

    override fun findById(id: String): ProjectDetails? {
        val request = HttpRequest.newBuilder().GET()
        val response = cobissClient.fetch("$endpoint/${URLEncoder.encode(id, "utf-8")}", request)
        val body = response.body()
        return try {
            cobissClient.json.decodeFromString(body)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}
