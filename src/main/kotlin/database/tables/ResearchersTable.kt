package database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

/**
 * @author Mihael Berčič on 12. 12. 23.
 */
object ResearchersTable : IdTable<Int>("researchers") {
    val firstName = varchar("first_name", 150)
    val lastName = varchar("last_name", 150)
    val title = varchar("title", 50)
    val sicrisID = integer("sicris_id").default(-1)
    val mstid = integer("mstid")
    val sex = bool("sex")
    val type = varchar("type", 100) // TODO: maybe change to enum later when I find out which types are possible
    val science = short("science")
    val subfield = varchar("subfield", 30)

    override val id: Column<EntityID<Int>> = mstid.entityId()
    override val primaryKey: PrimaryKey = PrimaryKey(mstid)
}

object ResearchersOrganisationTable : Table("researchers_organisation") {
    val researcher = reference("researcher", ResearchersTable)
    val organization = reference("organization", OrganizationTable)
    override val primaryKey: PrimaryKey = PrimaryKey(researcher, organization)
}

class ResearcherEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ResearcherEntity>(ResearchersTable)

    var firstName by ResearchersTable.firstName
    var lastName by ResearchersTable.lastName
    var title by ResearchersTable.title
    var mstid by ResearchersTable.mstid
    var sex by ResearchersTable.sex
    var type by ResearchersTable.type
    var science by ResearchersTable.science
    var subfield by ResearchersTable.subfield
    var sicrisID by ResearchersTable.sicrisID

    val projects by ProjectEntity via ProjectsResearcherTable
    var leadingProjects by ProjectEntity via ProjectLeadersTable
    var organisations by OrganizationEntity via ResearchersOrganisationTable
}