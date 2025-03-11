package database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

/**
 * @author Mihael Berčič on 3. 01. 24.
 */
object ProjectsTable : IntIdTable("projects_table") {
    val startDate = date("start")
    val endDate = date("end")
    val title = varchar("title", 500)
    val field = varchar("field", 10).nullable()
    val fte = double("FTE")
    val active = bool("active")
    val code = varchar("code", 500)
    val codeContract = varchar("codeContract", 500)
    val codeProgramme = varchar("codeProgramme", 500).nullable()
    val codeScience = varchar("codeScience", 500).nullable()
    val description = varchar("description", 10000).nullable()
    val frame = varchar("frame", 500)
    val fteHoursDescription = varchar("fteHoursDescription", 500)
    val hasTender = bool("hasTender")
    val name = varchar("name", 500)
    val researcherFullName = varchar("researcherFullName", 500)
    val stat = varchar("stat", 500)
    val statadm = varchar("statadm", 500)
    val statdate = varchar("statdate", 500)
    val type = varchar("type", 500)
    var projectSource = varchar("source", 100)
}

object ProjectsResearcherTable : Table("projects_researchers") {
    val project = reference("project", ProjectsTable)
    val researcher = reference("researcher", ResearchersTable)
    override val primaryKey: PrimaryKey = PrimaryKey(project, researcher)
}

object ProjectOrganizationsTable : Table("project_organizations") {
    val project = reference("project", ProjectsTable)
    val organization = reference("organization", OrganizationTable)
    override val primaryKey: PrimaryKey = PrimaryKey(project, organization)
}

object ProjectLeadersTable : Table("project_leaders") {
    val project = reference("project", ProjectsTable)
    val leader = reference("leader", ResearchersTable)
    override val primaryKey: PrimaryKey = PrimaryKey(project, leader)
}

class ProjectEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ProjectEntity>(ProjectsTable)

    var startDate by ProjectsTable.startDate
    var endDate by ProjectsTable.endDate
    var title by ProjectsTable.title
    var field by ProjectsTable.field
    var fte by ProjectsTable.fte
    var active by ProjectsTable.active
    var code by ProjectsTable.code
    var codeContract by ProjectsTable.codeContract
    var codeProgramme by ProjectsTable.codeProgramme
    var codeScience by ProjectsTable.codeScience
    var description by ProjectsTable.description
    var frame by ProjectsTable.frame
    var fteHoursDescription by ProjectsTable.fteHoursDescription
    var hasTender by ProjectsTable.hasTender
    var name by ProjectsTable.name
    var researcherFullName by ProjectsTable.researcherFullName
    var stat by ProjectsTable.stat
    var statadm by ProjectsTable.statadm
    var statdate by ProjectsTable.statdate
    var type by ProjectsTable.type
    var projectSource by ProjectsTable.projectSource

    var organizations by OrganizationEntity via ProjectOrganizationsTable
    var researchers by ResearcherEntity via ProjectsResearcherTable
}