import cobiss.CobissClient
import cobiss.Language
import cobiss.builder.project.ProjectDetails

fun main(args: Array<String>) {
    val username = args.getOrNull(0) ?: throw Exception("Missing username...")
    val password = args.getOrNull(1) ?: throw Exception("Missing password...")
    val client = CobissClient(username, password, "ecris", Language.Slovenian)
    val projectsQuery = client.projects()
        .search("Boosting local technology uptake")
        .limit(10)
        .build()

    val queryResponse = client.fetch(projectsQuery)
    println(queryResponse)
    val project = queryResponse.parse<ProjectDetails>()
    // projects.forEach(System.out::println)
    println(project.projectInformation)

}