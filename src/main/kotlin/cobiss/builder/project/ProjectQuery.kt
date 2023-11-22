package cobiss.builder.project

import cobiss.builder.CobissQuery
import cobiss.builder.QueryLimit

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
data class ProjectQuery(
    override val endpoint: String,
    override val searchQuery: String,
    override val limit: QueryLimit,
    override val id: String,
) : CobissQuery
