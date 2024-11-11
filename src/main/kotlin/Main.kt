import cobiss.CobissClient
import cobiss.Language
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

const val DELAY = 500L


fun main(args: Array<String>) {
    Logger.info("Usage: java -jar x.jar username password delay [modes = prepare, fetch]")
    Logger.info("If no modes are present, researchers are parsed. Mode prepare = setup BIB export. Mode fetch = download BIB export.")
    Database.connect("jdbc:sqlite:cobiss.db", "org.sqlite.JDBC")
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


    val researchers = transaction { ResearcherEntity.all().map { ResearcherID(it.sicrisID, it.mstid) } }
//        .filter { it.sicrisId == 41529 } // SANDI
    val bibliographyParser = BibliographyParser(modes, delay)
    bibliographyParser.parseBibliographies(researchers.map(ResearcherID::mstid))

    if (modes.isEmpty()) {
        researchers.forEach {
            val details = client.researchers.findById(it.sicrisId.toString()) ?: return@forEach
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
    Logger.info("\tStoring employments for researcher SICRIS: ${details.id} \t MSTID: $id")
    details.employs.forEach { employ ->
        val organisation = transaction { OrganizationEntity.findById(employ.orgid) }
        if (organisation != null) {
            Logger.info("${details.fullName} works at ${employ.orgName} for ${employ.rsrload}%");
            organisations.add(organisation)
        }
    }
    transaction { researcher.organisations = SizedCollection(organisations) }
}

private fun storeEducationForResearcher(details: ResearcherDetails) {
    val id = details.mstid.toInt()
    Logger.info("\tStoring educations for researcher SICRIS: ${details.id} \t MSTID: $id")
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
    Logger.info("\tStoring projects for researcher SICRIS: ${details.id} \t MSTID: ${details.mstid}")
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
            this.projectId = project.id
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
        val projectEntity = transaction { existingProject?.apply(statement) ?: ProjectEntity.new(statement) }
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
        this.organizationId = organization.id
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
    return transaction { existing?.apply(statement) ?: OrganizationEntity.new(statement) }
}

data class ResearcherID(val sicrisId: Int, val mstid: Int)