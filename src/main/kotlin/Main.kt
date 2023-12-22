import cobiss.CobissClient
import cobiss.Language
import cobiss.builder.QueryLimit
import database.tables.BibliographyUrl
import database.tables.BibliographyUrls
import database.tables.ResearcherEntity
import database.tables.ResearchersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
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
            SchemaUtils.createMissingTablesAndColumns(BibliographyUrls, ResearchersTable)
        } catch (_: java.lang.Exception) {
        }
    }

    val username = args.getOrNull(0) ?: throw Exception("Missing username...")
    val password = args.getOrNull(1) ?: throw Exception("Missing password...")
    val delay = args.getOrNull(2)?.toLong() ?: 500
    val modes = args.drop(3)
    val client = CobissClient(username, password, "ecris", Language.Slovenian)
    File("bibliographies").mkdir()

    getBibliographies(modes.contains("prepare"), modes.contains("fetch"), delay)

    val serializer: Serializer = Persister()
    val dataFetch = serializer.read(BibliographyPojo::class.java, File("bibliographies/36213.xml"))
    dataFetch.divisions.forEach {
        printDivisions(it)
    }
}

private fun printDivisions(division: BibliographyDivision) {
    division.divisions.forEach { printDivisions(it) }
    println(division.title)
    division.entryList?.entries?.forEach { entry ->
        println("${entry.title} [${entry.evaluation?.points?.toDoubleOrNull()}] => ${entry.authors?.authors?.joinToString { "${it.lastName} [${it.responsibility}]" }}")
    }
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

