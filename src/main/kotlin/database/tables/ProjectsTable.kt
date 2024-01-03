package database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

/**
 * @author Mihael Berčič on 3. 01. 24.
 */
object ProjectsTable : IdTable<Int>("projects_table") {
    val projectId = integer("project_id").uniqueIndex()
    val startDate = date("start")
    val endDate = date("end")
    val title = varchar("title", 255)
    val field = varchar("field", 10)
    val fte = double("FTE")
    override val id: Column<EntityID<Int>> = projectId.entityId()
}

object ProjectsResearcherTable : Table("projects_researchers") {
    val project = reference("project", ProjectsTable)
    val researcher = reference("researcher", ResearchersTable)
    override val primaryKey: PrimaryKey = PrimaryKey(project, researcher)
}

class ProjectEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ProjectEntity>(ProjectsTable)

    var projectId by ProjectsTable.projectId
    var startDate by ProjectsTable.startDate
    var endDate by ProjectsTable.endDate
    var title by ProjectsTable.title
    var field by ProjectsTable.field
    var fte by ProjectsTable.fte
    var researchers by ResearcherEntity via ProjectsResearcherTable
}