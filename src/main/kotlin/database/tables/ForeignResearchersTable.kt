package database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * @author Mihael Berčič on 26. 01. 24.
 */
object ForeignResearchersTable : IntIdTable("foreign_researchers") {
    val firstName = varchar("first_name", 150)
    val lastName = varchar("last_name", 150)
}

class ForeignResearcherEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ForeignResearcherEntity>(ForeignResearchersTable)

    var firstName by ForeignResearchersTable.firstName
    var lastName by ForeignResearchersTable.lastName
}