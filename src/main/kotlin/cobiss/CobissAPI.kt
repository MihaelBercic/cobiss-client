package cobiss

import java.net.URLEncoder
import java.net.http.HttpRequest

/**
 * @author Mihael Berčič on 29. 01. 24.
 */
interface CobissAPI<A, B> {
    val cobissClient: CobissClient
    val endpoint: String

    fun fetchId(id: String): String {
        val request = HttpRequest.newBuilder().GET()
        val response = cobissClient.fetch("$endpoint/${URLEncoder.encode(id, "utf-8")}", request)
        return response.body()
    }

    fun findById(id: String): A?
    fun newQuery(): B
}