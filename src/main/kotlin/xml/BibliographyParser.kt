package xml

import database.tables.*
import logging.Logger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Mihael Berčič on 29. 01. 24.
 */
class BibliographyParser() {

    private val blockingQueue = LinkedBlockingQueue<BibliographyDivision>()
    private val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("YYYYMMdd_HHmmss")
    private val timezone = ZoneId.of("UTC+1")

    private fun processDivision(division: BibliographyDivision, number: Number) {
        val total = division.entryList?.entries?.size ?: 0
        division.entryList?.entries?.forEachIndexed { index, entry ->
            time("[$number\t|\t$index \t/\t $total\t] Entry", false) {
                val url = entry.url?.trim()
                val summary = url?.let {
                    val document = Jsoup.connect(url).get()
                    document.select(".summary").firstOrNull()?.text()?.replace(" ... ", " ")
                }

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
                val language = entry.language?.joinToString()
                val keywords = entry.descriptors
                    .filter { it.lang == language }
                    .mapNotNull { it.content }
                    .plus(entry.topicalNames.orEmpty()).map(String::trim)


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
                    this.url = url
                    this.keywords = keywords.joinToString(",")
                    this.language = language
                    this.summary = summary
                }
                val orderedAuthors = slovenianAuthors.mapIndexed { index, s -> index to s }.toMap()
                val orderedForeignAuthors = foreignAuthors.mapIndexed { index, s -> index to s }.toMap()
                val paper = transaction { existingPaper?.apply(paperStatement) ?: PaperEntity.new(paperStatement) }


                orderedAuthors.forEach { (index, author) ->
                    transaction {
                        try {
                            PapersResearcherTable.insertIgnore {
                                it[PapersResearcherTable.paper] = paper.id
                                it[researcher] = author.id
                                it[position] = index
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
                            ForeignPapersResearcherTable.insertIgnore {
                                it[ForeignPapersResearcherTable.paper] = paper.id
                                it[researcher] = authorEntity.id
                                it[position] = index
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                Logger.info("Processed division number $number.", true)
            }
        }
    }

    public fun prepareAndFetch(modes: List<String>, fetchDelay: Long) {
        getBibliographies(modes.contains("prepare"), modes.contains("fetch"), fetchDelay)
    }

    fun parseBibliographies(mstids: List<Int>) {
        val atomic = AtomicInteger(0)
        mstids.chunked(100).forEachIndexed { index, chunk ->
            val threadPool = Executors.newFixedThreadPool(10)
            chunk.forEach { mstid ->
                try {
//                if (atomic.get() >= 100) {
//                    return@forEach
//                }
                    val serializer: Serializer = Persister()
                    val file = File("bibliographies/$mstid.xml")
                    if (!file.exists()) {
                        // Logger.error("Missing bibliographies/$mstid.xml")
                        return@forEach
                    }
                    // Logger.info("[${atomic.incrementAndGet()}] Parsing bibliographies/$mstid.xml")
                    val dataFetch = serializer.read(BibliographyPojo::class.java, file, false)
                    dataFetch.divisions.forEach {
                        storeDivision(it)
                    }
                } catch (e: Exception) {
                    Logger.error("Error happened when trying to read bibliography for $mstid...")
                    e.printStackTrace()
                }
            }
            Logger.info("Parser started! ${index * 100} / ${mstids.size}");
            while (blockingQueue.isNotEmpty()) {
                val division = blockingQueue.take()
                val number = blockingQueue.size
                threadPool.submit {
                    processDivision(division, number)
                }
            }
            threadPool.shutdown()
            threadPool.awaitTermination(10, TimeUnit.DAYS)
        }
        Logger.info("Finished processing bibliographies for ${mstids.size} researchers.")
    }

    private fun storeDivision(division: BibliographyDivision) {
        blockingQueue.add(division)
        division.divisions.forEach { storeDivision(it) }
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


    private fun <T> time(label: String, logTime: Boolean = true, block: () -> T): T {
        if (!logTime) {
            return block()
        }
        val start = System.currentTimeMillis()
        val value = block()
        val elapsed = System.currentTimeMillis() - start
        Logger.info("$label took ${elapsed}ms")
        return value
    }

    /**
     * Prepare and/or fetch the bibliographies. Use this instead of [fetchBibliographies] and [prepareBibliographies].
     */
    private fun getBibliographies(prepare: Boolean, fetch: Boolean, delay: Long) {
        val chunks = transaction { ResearcherEntity.all().map { it.id.value } }.chunked(120)
        chunks.forEach { chunk ->
            if (prepare) {
                chunk.forEach { id ->
                    try {
                        prepareBibliographyForResearcher(id, BibliographyOutputFormat.Xml)
                        Thread.sleep(delay)
                    } catch (e: Exception) {
                        Logger.info("Error happened [prepare] for $id")
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
                    Logger.error("Error happened [fetch] for $id")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Fetch the previously prepared bibliographies and store them on device in bibliographies folder.
     */
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

    /**
     * Request the bibliography computation on sicris servers.
     */
    private fun prepareBibliographies(delay: Long) {
        val researchers = transaction { ResearcherEntity.all().map { it.id.value } }
        researchers.forEach {
            try {
                prepareBibliographyForResearcher(it, BibliographyOutputFormat.Xml)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Thread.sleep(delay)
        }
    }


    /**
     * Individual fetch function for bibliography of a researcher.
     */
    private fun fetchBibliographyForResearcher(mstid: Int, outputFormat: BibliographyOutputFormat) {
        val bibliographyUrl = transaction { BibliographyUrl.findById(mstid) } ?: throw Exception("No such URL for $mstid.")
        val file = File("bibliographies/$mstid.${outputFormat.extension}")
        URI(bibliographyUrl.url).toURL().openStream().use { it.transferTo(file.outputStream()) }
    }

    /**
     * Individual prepare function for bibliography of a researcher.
     */
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
        Logger.debug("${"$mstid".padEnd(10)} @ $timeFormat => ${urlPath.padEnd(50)} ${response.statusCode()}")

        transaction {
            val existingBibliography = BibliographyUrl.findById(researcher.id.value)
            val statement: BibliographyUrl.() -> Unit = {
                this.researcher = researcher.id
                this.url = urlPath
                this.downloaded = false
            }
            existingBibliography?.apply(statement) ?: BibliographyUrl.new(statement)
        }

    }
}

fun makeHttpGetRequest(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
        connection.requestMethod = "GET"
        connection.connect()

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            "Error: ${connection.responseCode} ${connection.responseMessage}"
        }
    } finally {
        connection.disconnect()
    }
}