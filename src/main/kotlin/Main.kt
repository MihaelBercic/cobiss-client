import cobiss.CobissClient
import cobiss.Language
import cobiss.builder.project.Project

fun main(args: Array<String>) {
    val username = args.getOrNull(0) ?: throw Exception("Missing username...")
    val password = args.getOrNull(1) ?: throw Exception("Missing password...")
    val client = CobissClient(username, password, "ecris", Language.Slovenian)
    val projectsQuery = client.projects()
        .search("Boosting local technology uptake")
        .limit(10)
        .build()

    val queryResponse = client.fetch(projectsQuery)
    val projects = queryResponse.parse<List<Project>>()
    projects.forEach(System.out::println)

}