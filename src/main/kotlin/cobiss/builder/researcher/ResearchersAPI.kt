package cobiss.builder.researcher

import CobissAPI
import cobiss.CobissClient

/**
 * @author Mihael Berčič on 23. 11. 23.
 */
class ResearchersAPI(override val cobissClient: CobissClient) : CobissAPI<ResearcherDetails, ResearcherQueryBuilder> {

    override val endpoint: String = "researcher"

    override fun findById(id: String): ResearcherDetails? {
        return try {
            cobissClient.json.decodeFromString(fetchId(id))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun newQuery() = ResearcherQueryBuilder("researcher", cobissClient)
}