package cobiss.builder.organisation

import cobiss.CobissAPI
import cobiss.CobissClient
import cobiss.builder.researcher.ResearcherDetails

/**
 * @author Mihael Berčič on 23. 11. 23.
 */
class OrganisationAPI(override val cobissClient: CobissClient) : CobissAPI<OrganisationDetails, OrganisationQueryBuilder> {

    override val endpoint: String = "organization"

    override fun findById(id: String): OrganisationDetails? {
        return try {
            cobissClient.json.decodeFromString(fetchId(id))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun newQuery() = OrganisationQueryBuilder(endpoint, cobissClient)
}