import cobiss.CobissClient
import cobiss.Language
import cobiss.builder.project.Organization
import cobiss.builder.project.ProjectDetails
import cobiss.builder.researcher.ResearcherDetails
import database.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import xml.BibliographyParser
import java.io.File
import java.time.LocalDate

const val DELAY = 500L


fun main(args: Array<String>) {
    println("Usage: java -jar x.jar username password delay [modes = prepare, fetch]")
    Database.connect("jdbc:sqlite:cobiss.db", "org.sqlite.JDBC")
    try {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                ForeignResearchersTable, ForeignPapersResearcherTable, PapersResearcherTable, PapersTable, ProjectsTable, ProjectsResearcherTable, EducationsTable, BibliographyUrls, ResearchersTable, OrganizationTable, ProjectOrganizationsTable
            )
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }

    val username = args.getOrNull(0) ?: throw Exception("Missing username...")
    val password = args.getOrNull(1) ?: throw Exception("Missing password...")
    val modes = args.drop(2)
    val client = CobissClient(username, password, "ecris", Language.Slovenian)
    File("bibliographies").mkdir()

    /*
        val tosic = client.researchers.findById("41529") ?: throw Error("No researcher found in cobiss.")
        storeProjectsForResearcher(client, tosic)
        storeEducationForResearcher(tosic)
        BibliographyParser().parseBibliographies(listOf(tosic.mstid.toInt()))
        return
     */

    val bibliographyParser = BibliographyParser()
    val researchers = transaction { ResearcherEntity.all().map { ResearcherID(it.sicrisID, it.mstid) } }
    researchers.forEach {
        val details = client.researchers.findById(it.sicrisId.toString()) ?: return@forEach
        println("Storing projects for researcher SICRIS: ${it.sicrisId} \t MSTID: ${it.mstid}")
        storeProjectsForResearcher(client, details)
        Thread.sleep(DELAY)
        println("Storing educations for researcher SICRIS: ${it.sicrisId} \t MSTID: ${it.mstid}")
        storeEducationForResearcher(details)
        Thread.sleep(DELAY)
    }
    bibliographyParser.parseBibliographies(researchers.map(ResearcherID::mstid))
}

private fun storeEducationForResearcher(details: ResearcherDetails) {
    val id = details.mstid.toInt()
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
        transaction { existingProject?.apply(statement) ?: ProjectEntity.new(statement) }
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