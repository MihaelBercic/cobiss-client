package cobiss.builder.project

import cobiss.CobissClient
import cobiss.builder.QueryBuilder
import kotlinx.serialization.decodeFromString
import logging.Logger
import java.net.URLEncoder

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
class ProjectsQueryBuilder(private val endpoint: String, private val client: CobissClient) : QueryBuilder<Project, ProjectsQueryBuilder>() {

    private var offset = 0

    fun offset(amount: Int): ProjectsQueryBuilder {
        this.offset = amount
        return this
    }

    override fun fetch(): List<Project> {
        val endpoint = "$endpoint/search?query=${URLEncoder.encode(queryString, "utf-8")}&limit=${limit.representation}&offset=$offset"
        val response = client.fetch(endpoint)
        val body = response.body()
        return client.json.decodeFromString(body)
    }

}