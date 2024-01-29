package xml

import cobiss.CobissClient
import cobiss.builder.QueryLimit
import database.tables.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * @author Mihael Berčič on 29. 01. 24.
 */
class BibliographyParser {

    private val blockingQueue = LinkedBlockingQueue<BibliographyDivision>()
    private val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("YYYYMMdd_HHmmss")
    private val timezone = ZoneId.of("UTC+1")

    init {
        thread {
            println("Started bibliography parser queue thread!")
            while (true) {
                val division = blockingQueue.take()
                division.entryList?.entries?.forEach { entry ->
                    val title = entry.titleShort
                    val points = entry.evaluation?.points?.toDoubleOrNull() ?: 0.0
                    val authorNames = entry.authorsGroup?.authors ?: emptyList()
                    val slovenianAuthors = transaction { authorNames.mapNotNull { it.codeRes?.toIntOrNull()?.let(ResearcherEntity::findById) } ?: emptyList() }
                    val foreignAuthors = authorNames.filter { author -> slovenianAuthors.none { "${it.firstName}${it.lastName}" == "${author.firstName}${author.lastName}" } }
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
                    val orderedAuthors = slovenianAuthors.mapIndexed { index, s -> index to s }.toMap()
                    val orderedForeignAuthors = foreignAuthors.mapIndexed { index, s -> index to s }.toMap()
                    val paper = transaction { existingPaper?.apply(paperStatement) ?: PaperEntity.new(paperStatement) }
                    orderedAuthors.forEach { (index, author) ->
                        transaction {
                            try {
                                val relationExists = !PapersResearcherTable.select { (PapersResearcherTable.paper eq paper.id) and (PapersResearcherTable.researcher eq author.id) }.empty()
                                if (!relationExists) {
                                    PapersResearcherTable.insert {
                                        it[PapersResearcherTable.paper] = paper.id
                                        it[researcher] = author.id
                                        it[position] = index
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    orderedForeignAuthors.forEach { (index, author) ->
                        val existingEntity = transaction {
                            ForeignResearcherEntity.find {
                                (ForeignResearchersTable.firstName eq (author.firstName ?: "")) and (ForeignResearchersTable.lastName eq (author.lastName ?: ""))
                            }.firstOrNull()
                        }
                        val authorStatement: ForeignResearcherEntity.() -> Unit = {
                            firstName = author.firstName ?: "Unknown"
                            lastName = author.lastName ?: "Unknown"
                        }
                        val authorEntity = transaction { existingEntity?.apply(authorStatement) ?: ForeignResearcherEntity.new(authorStatement) }

                        transaction {
                            try {
                                val relationExists = !ForeignPapersResearcherTable.select { (ForeignPapersResearcherTable.paper eq paper.id) and (ForeignPapersResearcherTable.researcher eq authorEntity.id) }.empty()
                                if (!relationExists) {
                                    ForeignPapersResearcherTable.insert {
                                        it[ForeignPapersResearcherTable.paper] = paper.id
                                        it[researcher] = authorEntity.id
                                        it[position] = index
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    fun parseBibliographies(mstids: List<Int>) {
        mstids.forEach { mstid ->
            GlobalScope.launch {
                try {
                    val serializer: Serializer = Persister()
                    val dataFetch = serializer.read(BibliographyPojo::class.java, File("bibliographies/$mstid.xml"))
                    dataFetch.divisions.forEach {
                        storeDivision(it)
                    }
                } catch (e: Exception) {
                    println("Error happened when trying to read bibliography for $mstid...")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun storeDivision(division: BibliographyDivision) {
        blockingQueue.add(division)
        division.divisions.forEach { storeDivision(it) }
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
        val request =
            HttpRequest.newBuilder(URI("https://bib.cobiss.net/biblioweb/eval/si/slv/evalrsr/$mstid")).header("Content-Type", "application/x-www-form-urlencoded").header("Cookie", "JSESSIONID=-67qlC8bvTt_Q8sOr6kSoDcBlj1UzvDA81g8v_3g.praabw02")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build()

        val currentTime = LocalDateTime.now(timezone)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
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
        }/*
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
}