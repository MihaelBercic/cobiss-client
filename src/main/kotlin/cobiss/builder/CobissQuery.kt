package cobiss.builder

import java.net.URLEncoder

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
interface CobissQuery {
    val endpoint: String
    val searchQuery: String
    val limit: QueryLimit
    val id: String
    fun queryUrl(): String = id.ifEmpty { "search?query=${URLEncoder.encode(searchQuery, "utf-8")}&limit=${limit.representation}" }
}