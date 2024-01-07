import cobiss.CobissClient
import cobiss.Language
import cobiss.builder.QueryLimit
import cobiss.builder.project.ProjectDetails
import cobiss.builder.researcher.ResearcherDetails
import database.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import xml.BibliographyDivision
import xml.BibliographyPojo
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


private val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
private val dateTimeFormatter = DateTimeFormatter.ofPattern("YYYYMMdd_HHmmss")
private val timezone = ZoneId.of("UTC+1")

fun main(args: Array<String>) {
    println("Usage: java -jar x.jar username password delay [modes = prepare, fetch]")
    Database.connect("jdbc:sqlite:cobiss.db", "org.sqlite.JDBC")
    transaction {
        try {
            SchemaUtils.createMissingTablesAndColumns(
                PapersResearcherTable, PapersTable,
                ProjectsTable, ProjectsResearcherTable,
                EducationsTable,
                BibliographyUrls,
                ResearchersTable
            )
        } catch (_: java.lang.Exception) {
        }
    }

    val username = args.getOrNull(0) ?: throw Exception("Missing username...")
    val password = args.getOrNull(1) ?: throw Exception("Missing password...")
    val delay = args.getOrNull(2)?.toLong() ?: 500
    val modes = args.drop(3)
    val client = CobissClient(username, password, "ecris", Language.Slovenian)
    File("bibliographies").mkdir()

    // TODO: Testing
    // val tosic = client.researchers.findById("41529") ?: throw Error("No researcher found in cobiss.")
    // storeProjectsForResearcher(client, tosic)
    // storePapersForResearcher(tosic.mstid.toInt())
    // storePapersForResearcher(29488)
    // return

    getBibliographies(modes.contains("prepare"), modes.contains("fetch"), delay)
    // fetchResearchers(client)

    val allIds = transaction { ResearcherEntity.all().map { it.sicrisID to it.mstid }.filter { it.first > 0 } }

    allIds.forEach { (sicrisID, mstid) ->
        val researcher = client.researchers.findById("$sicrisID")
        if (researcher != null) {
            storeProjectsForResearcher(client, researcher)
            println("Stored Projects for $mstid")
            storePapersForResearcher(mstid)
            println("Stored Papers for $mstid")
        } else {
            println("No researcher details found for $sicrisID")
        }
    }
}

private fun storeEducationForResearcher(details: ResearcherDetails) {
    val id = details.mstid.toInt()
    val researcher = transaction { ResearcherEntity.findById(id) } ?: throw Error("No researcher found in db.")
    details.educations.forEach { data ->
        try {
            transaction {
                EducationsTable.insert {
                    it[EducationsTable.researcher] = researcher.id
                    it[title] = data.degree
                    it[code] = data.lvlcode
                    it[year] = data.year.toInt()
                    it[university] = data.university
                }
            }
        } catch (_: Exception) {

        }
    }
}

private fun storeProjectsForResearcher(client: CobissClient, details: ResearcherDetails) {
    val projectDetails = details.projects.mapNotNull { client.projects.findById("${it.id}") }
    projectDetails.forEach { project ->
        storeProjectInformation(project)
    }
}

private fun storeProjectInformation(project: ProjectDetails) {
    val title = project.name
    val startDate = LocalDate.parse(project.startdate)
    val endDate = LocalDate.parse(project.enddate)
    val fte = project.fteHoursDescription.split(" ")[0].toDoubleOrNull() ?: 0.0
    try {
        val existingProject = transaction { ProjectEntity.findById(project.id) }
        val researchers = transaction { project.researchers.mapNotNull { ResearcherEntity.findById(it.mstid.toInt()) } }
        val statement: ProjectEntity.() -> Unit = {
            this.projectId = project.id
            this.field = project.codeScience
            this.title = title
            this.startDate = startDate
            this.endDate = endDate
            this.fte = fte
            this.researchers = SizedCollection(researchers)
        }
        transaction { existingProject?.apply(statement) ?: ProjectEntity.new(statement) }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun storePapersForResearcher(mstid: Int) {
    val serializer: Serializer = Persister()
    val dataFetch = serializer.read(BibliographyPojo::class.java, File("bibliographies/$mstid.xml"))
    dataFetch.divisions.forEach {
        storeDivision(it)
    }
}

private fun storeDivision(division: BibliographyDivision) {
    division.entryList?.entries?.forEach { entry ->
        val title = entry.titleShort
        val points = entry.evaluation?.points?.toDoubleOrNull() ?: 0.0
        val authors = transaction { entry.authorsGroup?.authors?.mapNotNull { it.codeRes?.toIntOrNull()?.let(ResearcherEntity::findById) } ?: emptyList() }
        val publicationYear = entry.publicationYear?.toIntOrNull() ?: 1900
        val typology = entry.typology?.code ?: ""
        val doi = entry.identifier?.firstOrNull()?.dois?.firstOrNull()?.value ?: ""
        val publishedLocation = entry.bibSet?.firstOrNull()
        val publishedName = publishedLocation?.titleShort ?: ""
        val publishedISSN = entry.issn?.firstOrNull() ?: ""
        // println("$title [$points] => AUTHORS=${authors.size}, PUB=$publicationYear, TYPE=$typology, DOI=$doi, PUBNAME=$publishedName, PUBISSN=$publishedISSN")

        val existingPaper = transaction { PaperEntity.find { PapersTable.title eq title }.firstOrNull() }
        val paperStatement: PaperEntity.() -> Unit = {
            this.title = title
            this.points = points
            this.publicationYear = publicationYear
            this.typology = typology
            this.doi = doi
            this.publishedName = publishedName
            this.publishedISSN = publishedISSN
        }
        val orderedAuthors = authors.mapIndexed { index, s -> index to s }.toMap()
        val paper = transaction { existingPaper?.apply(paperStatement) ?: PaperEntity.new(paperStatement) }
        orderedAuthors.forEach { (index, author) ->
            transaction {
                try {
                    val relationExists = !PapersResearcherTable.select { (PapersResearcherTable.paper eq paper.id) and (PapersResearcherTable.researcher eq author.id) }.empty()
                    if (!relationExists) {
                        PapersResearcherTable.insert {
                            it[PapersResearcherTable.paper] = paper.id
                            it[PapersResearcherTable.researcher] = author.id
                            it[PapersResearcherTable.position] = index
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    division.divisions.forEach { storeDivision(it) }
}

private fun getBibliographies(prepare: Boolean, fetch: Boolean, delay: Long) {
    val chunks = transaction { ResearcherEntity.all().map { it.mstid } }.chunked(120)
    chunks.forEach { chunk ->
        if (prepare) {
            chunk.forEach { id ->
                try {
                    prepareBibliographyForResearcher(id, BibliographyOutputFormat.Xml)
                    Thread.sleep(delay)
                } catch (e: Exception) {
                    println("Error happened [prepare] for $id")
                    e.printStackTrace()
                }
            }
            Thread.sleep(delay * 3)
        }

        if (fetch) chunk.forEach { id ->
            try {
                fetchBibliographyForResearcher(id, BibliographyOutputFormat.Xml)
                Thread.sleep(delay)
            } catch (e: Exception) {
                println("Error happened [fetch] for $id")
                e.printStackTrace()
            }
        }
    }
}

private fun fetchBibliographies(delay: Long) {
    val urls = transaction { BibliographyUrl.all().toList() }
    val bibliographiesDirectory = File("bibliographies").mkdir()
    urls.forEach { bibliographyUrl ->
        val researcherId = transaction { bibliographyUrl.researcher.value }
        val file = File("bibliographies/$researcherId.xml")
        URI(bibliographyUrl.url).toURL().openStream().use { it.transferTo(file.outputStream()) }
        Thread.sleep(delay)
    }
}

private fun prepareBibliographies(delay: Long) {
    val researchers = transaction { ResearcherEntity.all().map { it.mstid } }
    researchers.forEach {
        try {
            prepareBibliographyForResearcher(it, BibliographyOutputFormat.Xml)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Thread.sleep(delay)
    }
}

private fun fetchBibliographyForResearcher(mstid: Int, outputFormat: BibliographyOutputFormat) {
    val bibliographyUrl = transaction { BibliographyUrl.findById(mstid) } ?: throw Exception("No such URL for $mstid.")
    val file = File("bibliographies/$mstid.${outputFormat.extension}")
    URI(bibliographyUrl.url).toURL().openStream().use { it.transferTo(file.outputStream()) }
}

private fun prepareBibliographyForResearcher(mstid: Int, outputFormat: BibliographyOutputFormat) {
    val researcher = transaction { ResearcherEntity.findById(mstid) } ?: throw NoSuchElementException("No researcher with mstid: $mstid")
    val title = researcher.title.takeIf { it.isNotBlank() }?.plus('+') ?: ""
    val firstName = researcher.firstName.split(" ").joinToString("+") { URLEncoder.encode(it, Charset.defaultCharset()) }
    val lastName = researcher.lastName.split(" ").joinToString("+") { URLEncoder.encode(it, Charset.defaultCharset()) }
    val code = URLEncoder.encode("[$mstid]", Charset.defaultCharset())

    val form = "fullName=$title$firstName+$lastName+$code&uniqueCode=$mstid&biblioUrl=&errorMsg=&researchers=&fromYear=&toYear=&biblioFormat=ISO&outputFormat=${outputFormat.abbreviation}&science=T&altmetrics=none&unit=ZS&email="
    val request = HttpRequest
        .newBuilder(URI("https://bib.cobiss.net/biblioweb/eval/si/slv/evalrsr/$mstid"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Cookie", "JSESSIONID=-67qlC8bvTt_Q8sOr6kSoDcBlj1UzvDA81g8v_3g.praabw02")
        .POST(BodyPublishers.ofString(form)).build()

    val currentTime = LocalDateTime.now(timezone)
    val response = httpClient.send(request, BodyHandlers.discarding());
    val timeFormat = currentTime.format(dateTimeFormatter)
    val urlPath = response.uri().toURL().toString()
    println("${"$mstid".padEnd(10)} @ $timeFormat => ${urlPath.padEnd(50)} ${response.statusCode()}")

    transaction {
        val existingBibliography = BibliographyUrl.findById(researcher.id.value)
        val statement: BibliographyUrl.() -> Unit = {
            this.researcher = researcher.id
            this.url = urlPath
            this.downloaded = false
        }
        existingBibliography?.apply(statement) ?: BibliographyUrl.new(statement)
    }
    /*
    val decoder = XMLDecoder(localTmpXMLFile.inputStream()).use {
         println(it.readObject())
     }
     */

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


enum class BibliographyOutputFormat(val abbreviation: Char, val extension: String) {
    Html('H', "html"), Xml('X', "xml")
}

private fun fetchResearchers(client: CobissClient) {
    val researchers = client.researchers.newQuery().limit(QueryLimit.All).fetch()
    researchers.forEach { researcher ->
        val firstName = researcher.firstName
        val lastName = researcher.lastName
        val title = researcher.title
        val science = researcher.science
        val sex = researcher.sex
        val subfield = researcher.subfield
        val type = researcher.type
        val typeDescription = researcher.typeDescription
        val mstid = researcher.mstid.toInt()

        transaction {
            val researcherEntityStatement: ResearcherEntity.() -> Unit = {
                this.firstName = firstName
                this.lastName = lastName
                this.title = title
                this.science = science.toShortOrNull() ?: -1
                this.sex = sex == "M"
                this.subfield = subfield
                this.type = type
                this.mstid = mstid
                this.sicrisID = researcher.id
            }
            val existingResearcher = ResearcherEntity.findById(mstid)
            existingResearcher?.apply(researcherEntityStatement) ?: ResearcherEntity.new(researcherEntityStatement)
        }
        println("Finished adding ${researcher.mstid} to researchers.")
    }
}


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