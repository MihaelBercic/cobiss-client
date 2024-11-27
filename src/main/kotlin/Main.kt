import cobiss.CobissClient
import cobiss.Language
import cobiss.builder.QueryLimit
import cobiss.builder.project.Organization
import cobiss.builder.project.ProjectDetails
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
    if (transaction { ResearcherEntity.all().empty() }) {
        Logger.info("Fetching researchers to populate the database!")
        fetchResearchers(client)
    }
    if (transaction { OrganizationEntity.all().empty() }) {
        Logger.info("Fetching organisations to populate the database!")
        fetchOrganisations(client)
    }
    val researchers = transaction { ResearcherEntity.all().map { ResearcherID(it.sicrisID, it.id.value) } }
    bibliographyParser.apply {
        prepareAndFetch(modes, delay)
        parseBibliographies(researchers.map(ResearcherID::mstid))
    }
//        .filter { it.sicrisId == 41529 } // SANDI
    if (modes.isEmpty()) {
        val total = researchers.count()
        var current = 0
        researchers.forEach {
            Logger.info("${++current} / $total")
            val details = client.researchers.findById(it.sicrisId.toString()) ?: return@forEach
            val id = details.mstid.toInt()
            Logger.info("SICRIS: ${details.id} \t MSTID: $id")
            storeProjectsForResearcher(client, details)
            storeEducationForResearcher(details)
            storeOrganisationsForResearcher(details)
            Thread.sleep(DELAY)
        }
    }
    Logger.info("Done.");
    /*
        val tosic = client.researchers.findById("41529") ?: throw Error("No researcher found in cobiss.")
        storeProjectsForResearcher(client, tosic)
        storeEducationForResearcher(tosic)
        BibliographyParser().parseBibliographies(listOf(tosic.mstid.toInt()))
        return
     */
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
    val projectDetails = details.projects.mapNotNull {
        val details = client.projects.findById("${it.id}")
        Thread.sleep(DELAY)
        return@mapNotNull details
    }
    projectDetails.forEach { project ->
        storeProjectInformation(project)
    }
}

private fun storeProjectInformation(project: ProjectDetails) {
    val title = project.name
    val startDate = LocalDate.parse(project.startdate)
    val endDate = LocalDate.parse(project.enddate)
    val fte = project.fteHoursDescription.split(" ")[0].toDoubleOrNull() ?: 0.0
    val leader = transaction {
        ResearcherEntity.find {
            concat(ResearchersTable.title, stringLiteral(" "), ResearchersTable.firstName, stringLiteral(" "), ResearchersTable.lastName) eq project.resaercherFullName
        }.firstOrNull()
    }
    try {
        val existingProject = transaction { ProjectEntity.findById(project.id) }
        val researchers = transaction { project.researchers.mapNotNull { ResearcherEntity.findById(it.mstid.toInt()) } }
        val statement: ProjectEntity.() -> Unit = {
            this.startDate = startDate
            this.endDate = endDate
            this.title = title
            this.field = project.codeScience
            this.fte = fte
            this.active = project.active
            this.code = project.code
            this.codeContract = project.codeContract
            this.codeProgramme = project.codeProgramme
            this.codeScience = project.codeScience
            this.description = project.description
            this.frame = project.frame
            this.fteHoursDescription = project.fteHoursDescription
            this.hasTender = project.hasTender
            this.name = project.name
            this.researcherFullName = project.resaercherFullName
            this.stat = project.stat
            this.statadm = project.statadm
            this.statdate = project.statdate
            this.type = project.type

            this.organizations = SizedCollection(project.organizations.mapNotNull { findOrCreateOrganization(it) })
            this.researchers = SizedCollection(researchers)
        }
        val projectEntity = transaction { existingProject?.apply(statement) ?: ProjectEntity.new(project.id, statement) }
        if (leader != null) {
            val leadingProjects = leader.leadingProjects ?: mutableListOf<ProjectEntity>()
            transaction {
                leader.leadingProjects = SizedCollection(leadingProjects.plus(projectEntity))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun findOrCreateOrganization(organization: Organization): OrganizationEntity? {
    if (organization.id == null) return null
    val existing = transaction { OrganizationEntity.findById(organization.id) }
    val statement: OrganizationEntity.() -> Unit = {
        this.frame = organization.frame
        this.stat = organization.stat
        this.statadm = organization.statadm
        this.statdate = organization.statdate
        this.type = organization.type
        this.counter = organization.counter
        this.field = organization.field
        this.science = organization.science
        this.subfield = organization.subfield
        this.city = organization.city
        this.mstid = organization.mstid
        this.name = organization.name
        this.regnum = organization.regnum
        this.remark = organization.remark
        this.rolecode = organization.rolecode
        this.sigla = organization.sigla
        this.ssm = organization.ssm
        this.statfrm = organization.statfrm
        this.taxnum = organization.taxnum
    }
    return transaction { existing?.apply(statement) ?: OrganizationEntity.new(organization.id, statement) }
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
        val typeDescription = researcher.typeDescription
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