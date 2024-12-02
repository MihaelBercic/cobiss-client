package database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

/**
 * @author Mihael Berčič on 3. 01. 24.
 */
object ProjectsTable : IntIdTable("projects_table") {
    val startDate = date("start")
    val endDate = date("end")
    val title = varchar("title", 255)
    val field = varchar("field", 10).nullable()
    val fte = double("FTE")
    val active = bool("active")
    val code = varchar("code", 255)
    val codeContract = varchar("codeContract", 255)
    val codeProgramme = varchar("codeProgramme", 255).nullable()
    val codeScience = varchar("codeScience", 255).nullable()
    val description = varchar("description", 10000).nullable()
    val frame = varchar("frame", 255)
    val fteHoursDescription = varchar("fteHoursDescription", 255)
    val hasTender = bool("hasTender")
    val name = varchar("name", 255)
    val researcherFullName = varchar("researcherFullName", 255)
    val stat = varchar("stat", 255)
    val statadm = varchar("statadm", 255)
    val statdate = varchar("statdate", 255)
    val type = varchar("type", 255)
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

    var organizations by OrganizationEntity via ProjectOrganizationsTable
    var researchers by ResearcherEntity via ProjectsResearcherTable
}