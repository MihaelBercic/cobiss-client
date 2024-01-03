package database.tables

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

/**
 * @author Mihael Berčič on 3. 01. 24.
 */
object PapersTable : IdTable<String>("papers") {

    // val points = double("points")
    // val authors =
    val title = varchar("title", 255)
    val publicationYear = integer("publication_year")
    val typology = varchar("typology", 30)
    val doi = varchar("doi", 30).uniqueIndex()
    val publishedName = varchar("publication_name", 255)
    val publishedISSN = varchar("publication_issn", 30)
    val points = double("points")

    override val id: Column<EntityID<String>> = doi.entityId()
}

class PaperEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, PaperEntity>(PapersTable)

    var title by PapersTable.title
    var publicationYear by PapersTable.publicationYear
    var typology by PapersTable.typology
    var doi by PapersTable.doi
    var publishedName by PapersTable.publishedName
    var publishedISSN by PapersTable.publishedISSN
    var points by PapersTable.points
}

object PapersResearcherTable : Table("papers_researchers") {
    val researcher = reference("researcher", ResearchersTable)
    val paper = reference("paper", PapersTable)
    val position = integer("list_position")
}