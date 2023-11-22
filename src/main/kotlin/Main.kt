import cobiss.CobissClient
import cobiss.Language
import cobiss.builder.QueryBuilder

fun main(args: Array<String>) {
    val username = args.getOrNull(0) ?: throw Exception("Missing username...")
    val password = args.getOrNull(1) ?: throw Exception("Missing password...")
    val client = CobissClient(username, password, "ecris", Language.Slovenian)
    val projects = client.projects.newQuery()
        .search("Bootstrap")
        .limit(10)
        .fetch()

    val project = client.projects.findById("7576")
}


interface CobissAPI<A, B>{
    val endpoint: String
    fun findById(id: String): A
    fun newQuery(): QueryBuilder<B>
}

