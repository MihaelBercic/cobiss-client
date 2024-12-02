import cobiss.CobissClient
import cobiss.Language
import cobiss.builder.QueryLimit
import cobiss.builder.researcher.ResearcherDetails
import database.tables.*
import logging.Logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import xml.BibliographyParser
import java.io.File
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


const val DELAY = 500L


fun main(args: Array<String>) {
    Logger.info("Usage: java -jar x.jar username password delay [modes = prepare, fetch]")
    Logger.info("If no modes are present, researchers are parsed. Mode prepare = setup BIB export. Mode fetch = download BIB export.")
    Database.connect(
        "jdbc:postgresql://localhost:5432/postgres",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "cobiss"
    )
    try {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                ForeignResearchersTable,
                ForeignPapersResearcherTable,
                PapersResearcherTable,
                PapersTable,
                ProjectsTable,
                ProjectsResearcherTable,
                EducationsTable,
                BibliographyUrls,
                ResearchersTable,
                OrganizationTable,
                ProjectOrganizationsTable,
                ResearchersOrganisationTable,
                ProjectLeadersTable
            )
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }

    val username = args.getOrNull(0) ?: throw Exception("Missing username...")
    val password = args.getOrNull(1) ?: throw Exception("Missing password...")
    val delay = args.getOrNull(2)?.toLongOrNull() ?: throw Exception("Missing delay...")
    val modes = args.drop(3)
    val client = CobissClient(username, password, "ecris", Language.Slovenian)
    File("bibliographies").mkdir()
    val bibliographyParser = BibliographyParser()


//        val tosic = client.researchers.findById("41529") ?: throw Error("No researcher found in cobiss.")
//        storeProjectsForResearcher(client, tosic)
//        storeEducationForResearcher(tosic)
//        BibliographyParser().parseBibliographies(listOf(tosic.mstid.toInt()))
//        return


    if (transaction { ResearcherEntity.all().empty() }) {
        Logger.info("Fetching researchers to populate the database!")
        fetchResearchers(client)
    }
    if (transaction { OrganizationEntity.all().empty() }) {
        Logger.info("Fetching organisations to populate the database!")
        fetchOrganisations(client)
    }
    if (transaction { ProjectEntity.all().empty() }) {
        Logger.info("Fetching projects to populate the database!")
        fetchProjects(client)
    }

    val researchers = transaction { ResearcherEntity.all().map { ResearcherID(it.sicrisID, it.id.value) } }
    bibliographyParser.apply {
        prepareAndFetch(modes, delay)
        parseBibliographies(researchers.map(ResearcherID::mstid))
    }
    if (modes.isEmpty()) {
        val total = researchers.count()
        var current = 0
        researchers.forEach {
            Logger.info("${++current} / $total")
            val details = client.researchers.findById(it.sicrisId.toString()) ?: return@forEach
            val id = details.mstid.toInt()
            Logger.info("MSTID: $id")
            storeProjectsForResearcher(client, details)
            storeEducationForResearcher(details)
            storeOrganisationsForResearcher(details)
            Thread.sleep(DELAY)
        }
    }
    Logger.info("Done.");
}

private fun storeOrganisationsForResearcher(details: ResearcherDetails) {
    val id = details.mstid.toInt()
    val researcher = transaction { ResearcherEntity.findById(id) } ?: throw Error("No researcher found in db.")
    val organisations = mutableListOf<OrganizationEntity>()
    Logger.info("\tEmployments...")
    details.employs.forEach { employ ->
        val organisation = transaction { OrganizationEntity.findById(employ.orgCode.toInt()) }
        if (organisation != null) {
            Logger.info("\t${details.fullName} works at ${employ.orgName} for ${employ.rsrload}%");
            organisations.add(organisation)
        } else {
            Logger.error("\tUnable to find the organisation: ${employ}!")
        }
    }
    transaction { researcher.organisations = SizedCollection(organisations) }
}

private fun storeEducationForResearcher(details: ResearcherDetails) {
    val id = details.mstid.toInt()
    Logger.info("\tEducations...")
    val researcher = transaction { ResearcherEntity.findById(id) } ?: throw Error("No researcher found in db.")
    details.educations.forEach { data ->
        try {
            transaction {
                EducationsTable.insertIgnore {
                    it[EducationsTable.researcher] = researcher.id
                    it[title] = data.degree
                    it[code] = data.lvlcode
                    it[year] = data.year.toInt()
                    it[university] = data.university
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun storeProjectsForResearcher(client: CobissClient, details: ResearcherDetails) {
    Logger.info("\tProjects...")
    val allProjects = details.projects.plus(details.internacionalprojects).mapNotNull {
        Thread.sleep(DELAY)
        return@mapNotNull client.projects.findById("${it.id}")
    }
    val ourEntity = transaction { ResearcherEntity.findById(details.mstid.toInt()) } ?: throw Error("No researcher found in db.")
    allProjects.forEach { project ->
        try {
            val existingProject = transaction { ProjectEntity.findById(project.id) } ?: throw Error("Project not found!")
            val researchers = transaction { project.researchers.mapNotNull { ResearcherEntity.findById(it.mstid.toInt()) } }
            val projectEntity = transaction { existingProject.researchers = SizedCollection(researchers.plus(ourEntity)) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class ResearcherID(val sicrisId: Int, val mstid: Int)

/**
 * Store all researchers in the database.
 */
fun fetchResearchers(client: CobissClient) {
    val researchers = client.researchers.newQuery().limit(QueryLimit.All).fetch()
    val threadPool = Executors.newFixedThreadPool(20)
    researchers.forEach { researcher ->
        val firstName = researcher.firstName
        val lastName = researcher.lastName
        val title = researcher.title
        val science = researcher.science
        val sex = researcher.sex
        val subfield = researcher.subfield
        val type = researcher.type
        val mstid = researcher.mstid.toInt()

        threadPool.submit {
            transaction {
                val researcherEntityStatement: ResearcherEntity.() -> Unit = {
                    this.firstName = firstName
                    this.lastName = lastName
                    this.title = title
                    this.science = science.toShortOrNull() ?: -1
                    this.sex = sex == "M"
                    this.subfield = subfield
                    this.type = type
                    this.sicrisID = researcher.id
                }
                val existingResearcher = ResearcherEntity.findById(mstid)
                existingResearcher?.apply(researcherEntityStatement) ?: ResearcherEntity.new(mstid, researcherEntityStatement)
            }
        }
        Logger.info("Finished adding ${researcher.mstid} to researchers.")
    }
    threadPool.shutdown()
    threadPool.awaitTermination(1, TimeUnit.HOURS)
    Logger.debug("Finished researcher fetch!")
}

/**
 * Store all organisations in the database.
 */
fun fetchOrganisations(client: CobissClient) {
    val organisations = client.organisations.newQuery().limit(QueryLimit.All).fetch()
    val threadPool = Executors.newFixedThreadPool(20)
    organisations.forEach { organisation ->
        val mstid = organisation.mstid.toInt()
        threadPool.submit {
            transaction {
                val organisationEntityStatement: OrganizationEntity.() -> Unit = {
                    this.frame = organisation.frame
                    this.stat = organisation.stat
                    this.statadm = organisation.statAdn
                    this.statdate = organisation.statDate
                    this.type = organisation.type
                    this.field = organisation.field
                    this.science = organisation.science
                    this.subfield = organisation.subfield
                    this.city = organisation.city
                    this.mstid = organisation.mstid
                    this.name = organisation.name
                    this.regnum = organisation.regnum
                    this.statfrm = organisation.statFrm
                    this.taxnum = organisation.taxNumber
                }
                val existingResearcher = OrganizationEntity.findById(mstid)
                existingResearcher?.apply(organisationEntityStatement) ?: OrganizationEntity.new(mstid, organisationEntityStatement)
            }
            Logger.info("Finished adding ${organisation.mstid} to organisations.")
        }
    }
    threadPool.shutdown()
    threadPool.awaitTermination(1, TimeUnit.HOURS)
    Logger.debug("Finished organisation fetch!")
}

/**
 * Store all projects in the database.
 */
fun fetchProjects(client: CobissClient) {
    val projects = client.projects.newQuery().limit(QueryLimit.All).fetch()
    val threadPool = Executors.newFixedThreadPool(20)
    projects.forEach { project ->
        val id = project.id ?: return@forEach
        threadPool.submit {
            val details = client.projects.findById(id.toString()) ?: return@submit
            val title = details.name
            val startDate = LocalDate.parse(details.startdate)
            val endDate = LocalDate.parse(details.enddate)
            val fte = details.fteHoursDescription.split(" ")[0].toDoubleOrNull() ?: 0.0
            val existingProject = transaction { ProjectEntity.findById(id) }
            val statement: ProjectEntity.() -> Unit = {
                this.startDate = startDate
                this.endDate = endDate
                this.title = title
                this.field = details.codeScience
                this.fte = fte
                this.active = details.active
                this.code = details.code
                this.codeContract = details.codeContract
                this.codeProgramme = details.codeProgramme
                this.codeScience = details.codeScience
                this.description = details.description
                this.frame = details.frame
                this.fteHoursDescription = details.fteHoursDescription
                this.hasTender = details.hasTender
                this.name = details.name
                this.researcherFullName = details.resaercherFullName
                this.stat = details.stat
                this.statadm = details.statadm
                this.statdate = details.statdate
                this.type = details.type

                this.organizations = SizedCollection(details.organizations.mapNotNull { OrganizationEntity.findById(it.id) })
                this.researchers = SizedCollection()
            }
            val projectEntity = transaction { existingProject?.apply(statement) ?: ProjectEntity.new(details.id, statement) }

            // Attempt to find leader using title first (dr. Mihael Berčič) and if unsuccessful without the title. (Mihael Berčič).
            val leader = transaction {
                ResearcherEntity.find {
                    concat(ResearchersTable.title, stringLiteral(" "), ResearchersTable.firstName, stringLiteral(" "), ResearchersTable.lastName) eq details.resaercherFullName
                }.firstOrNull()
                    ?: ResearcherEntity.find {
                        concat(ResearchersTable.firstName, stringLiteral(" "), ResearchersTable.lastName) eq details.resaercherFullName
                    }.firstOrNull()
            }
            Logger.debug("Leader: ${leader?.firstName} ${leader?.lastName} for: ${details.resaercherFullName}")
            if (leader != null) {
                val leadingProjects = leader.leadingProjects
                transaction {
                    leader.leadingProjects = SizedCollection(leadingProjects.plus(projectEntity))
                }
            }
            Logger.info("Finished adding ${details.id} to projects.")
        }
    }
    threadPool.shutdown()
    threadPool.awaitTermination(1, TimeUnit.HOURS)
    Logger.debug("Finished organisation fetch!")
}