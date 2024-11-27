package database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

/**
 * @author Mihael Berčič on 29. 01. 24.
 */
object OrganizationTable : IntIdTable("organization") {
    val frame = varchar("frame", 255).nullable()
    val stat = varchar("stat", 255).nullable()
    val statadm = varchar("statadm", 255).nullable()
    val statdate = varchar("statdate", 255).nullable()
    val type = varchar("type", 255).nullable()
    val counter = varchar("counter", 255).nullable()
    val field = varchar("field", 255).nullable()
    val science = varchar("science", 255).nullable()
    val subfield = varchar("subfield", 255).nullable()
    val city = varchar("city", 255).nullable()
    val mstid = varchar("mstid", 255).nullable()
    val name = varchar("name", 255).nullable()
    val regnum = varchar("regnum", 255).nullable()
    val remark = varchar("remark", 255).nullable()
    val rolecode = varchar("rolecode", 255).nullable()
    val sigla = varchar("sigla", 255).nullable()
    val ssm = varchar("ssm", 255).nullable()
    val statfrm = varchar("statfrm", 255).nullable()
    val taxnum = varchar("taxnum", 255).nullable()
}

class OrganizationEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<OrganizationEntity>(OrganizationTable)

    var frame by OrganizationTable.frame
    var stat by OrganizationTable.stat
    var statadm by OrganizationTable.statadm
    var statdate by OrganizationTable.statdate
    var type by OrganizationTable.type
    var counter by OrganizationTable.counter
    var field by OrganizationTable.field
    var science by OrganizationTable.science
    var subfield by OrganizationTable.subfield
    var city by OrganizationTable.city
    var mstid by OrganizationTable.mstid
    var name by OrganizationTable.name
    var regnum by OrganizationTable.regnum
    var remark by OrganizationTable.remark
    var rolecode by OrganizationTable.rolecode
    var sigla by OrganizationTable.sigla
    var ssm by OrganizationTable.ssm
    var statfrm by OrganizationTable.statfrm
    var taxnum by OrganizationTable.taxnum
}