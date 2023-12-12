import cobiss.CobissClient
import cobiss.Language
import cobiss.builder.QueryBuilder
import cobiss.builder.QueryLimit
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.net.http.HttpRequest


fun main(args: Array<String>) {
    val username = args.getOrNull(0) ?: throw Exception("Missing username...")
    val password = args.getOrNull(1) ?: throw Exception("Missing password...")
    val client = CobissClient(username, password, "ecris", Language.Slovenian)
    val researchers = client.researchers.newQuery()
        .limit(QueryLimit.All)
        .fetch()
    researchers.forEach{ println("${it.firstName} ${it.lastName} ${it.fullName} ${it.title} ${it.mstid} ${it.id}")}
    println(researchers.size);
}

/**
 * 1. Store all researchers in the database
 * 2.
 * 3.
 * 4. https://bib.cobiss.net/biblioweb/eval/si/slv/evalrsr/{mstid}
 *      - send FORM to ^ url
 *      - take current time (YYYYMMDD_HHMMSS_{mstid}
 *      - get xml from https://bib.cobiss.net/bibliographics/si/webBiblio/bib301_{YYYYMMDD}_{HHMMSS}_{mstid}.(xml / html)
 *
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

