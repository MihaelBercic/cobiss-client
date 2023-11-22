import cobiss.builder.QueryLimit
import cobiss.builder.project.Project
import java.net.http.HttpClient
import java.net.http.HttpRequest

fun main(args: Array<String>) {
    val username = args.getOrNull(0) ?: throw Exception("Missing username...")
    val password = args.getOrNull(1) ?: throw Exception("Missing password...")
    // val client = CobissClient(username, password, "ecris", Language.Slovenian)

}

object Projects : APICommunicator() {

    fun findById(id: String) {
        val request = id // ...
    }
}

class ProjectsQueryBuilder : QueryBuilder<Project>() {

    override fun fetch(): List<Project> {
        val request = HttpRequest.newBuilder()
    }

}

abstract class QueryBuilder<T> : APICommunicator() {
    protected var queryString = "*"
    protected var limit: QueryLimit = QueryLimit.All

    fun search(query: String): QueryBuilder<T> {
        this.queryString = query
        return this
    }

    fun limit(quantity: Int): QueryBuilder<T> {
        this.limit = QueryLimit.Some(quantity)
        return this
    }

    abstract fun fetch(): List<T>

}

open class APICommunicator {
    protected val httpClient = HttpClient.newHttpClient()
}