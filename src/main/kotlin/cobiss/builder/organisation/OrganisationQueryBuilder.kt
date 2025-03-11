package cobiss.builder.organisation

import cobiss.CobissClient
import cobiss.builder.QueryBuilder
import java.net.URLEncoder

/**
 * @author Mihael Berčič on 23. 11. 23.
 */
class OrganisationQueryBuilder(private val endpoint: String, private val client: CobissClient) : QueryBuilder<OrganisationDetails, OrganisationQueryBuilder>() {

    override fun fetch(): List<OrganisationDetails> {
        val endpoint = "$endpoint/search?query=${URLEncoder.encode(queryString, "utf-8")}&limit=${limit.representation}"
        val response = client.fetch(endpoint)
        val body = response.body()
        return client.json.decodeFromString(body)
    }
}