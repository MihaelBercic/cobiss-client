package database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table

/**
 * @author Mihael Berčič on 3. 01. 24.
 */
object PapersTable : IntIdTable("papers") {

    // val points = double("points")
    // val authors =
    val title = text("title")
    val publicationYear = integer("publication_year")
    val typology = varchar("typology", 81)
    val doi = varchar("doi", 82)
    val publishedName = text("publication_name")
    val publishedISSN = varchar("publication_issn", 83)
    val points = double("points")
    val keywords = text("keywords").nullable()
    val url = varchar("url", 255).nullable()
}

class PaperEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PaperEntity>(PapersTable)

    var title by PapersTable.title
    var publicationYear by PapersTable.publicationYear
    var typology by PapersTable.typology
    var doi by PapersTable.doi
    var publishedName by PapersTable.publishedName
    var publishedISSN by PapersTable.publishedISSN
    var points by PapersTable.points
    var keywords by PapersTable.keywords
    var url by PapersTable.url
}

object PapersResearcherTable : Table("papers_researchers") {
    val researcher = reference("researcher", ResearchersTable)
    val paper = reference("paper", PapersTable)
    val position = integer("list_position")
    override val primaryKey: PrimaryKey = PrimaryKey(researcher, paper)
}

object ForeignPapersResearcherTable : Table("foreign_papers_researchers") {
    val researcher = reference("researcher", ForeignResearchersTable)
    val paper = reference("paper", PapersTable)
    val position = integer("list_position")
    override val primaryKey: PrimaryKey = PrimaryKey(researcher, paper)
}