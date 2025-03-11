package database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Table

/**
 * @author Mihael Berčič on 3. 01. 24.
 */
object EducationsTable : Table("educations") {
    val researcher = reference("researcher", ResearchersTable)
    val title = varchar("title", 80)
    val year = integer("year")
    val university = varchar("university", 130)
    val code = varchar("code", 10)
    override val primaryKey: PrimaryKey = PrimaryKey(researcher, code)
}