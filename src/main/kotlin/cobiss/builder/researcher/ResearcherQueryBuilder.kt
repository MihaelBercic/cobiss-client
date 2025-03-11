package cobiss.builder.researcher

import cobiss.CobissClient
import cobiss.builder.QueryBuilder
import java.net.URLEncoder

/**
 * @author Mihael Berčič on 23. 11. 23.
 */
class ResearcherQueryBuilder(private val endpoint: String, private val client: CobissClient) : QueryBuilder<Researcher, ResearcherQueryBuilder>() {

    override fun fetch(): List<Researcher> {
        val endpoint = "$endpoint/search?query=${URLEncoder.encode(queryString, "utf-8")}&limit=${limit.representation}"
        val response = client.fetch(endpoint)
        val body = response.body()
        return client.json.decodeFromString(body)
    }
}