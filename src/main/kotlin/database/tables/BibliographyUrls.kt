package database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

/**
 * @author Mihael Berčič on 13. 12. 23.
 */
object BibliographyUrls : IdTable<Int>("bibliography_urls") {
    val researcher = reference("researcher", ResearchersTable)
    val url = varchar("url", 150)
    val downloaded = bool("downloaded")
    override val id: Column<EntityID<Int>> = researcher
}

class BibliographyUrl(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BibliographyUrl>(BibliographyUrls)

    var researcher by BibliographyUrls.researcher
    var url by BibliographyUrls.url
    var downloaded by BibliographyUrls.downloaded
}