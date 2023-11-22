package cobiss.builder.project

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
@Serializable
data class Project(
    val frame: String,
    val id: Int,
    val stat: String,
    @SerialName("statadm") val statADM: String,
    @SerialName("statdate") val statDate: String,
    val type: String,
    val classificationDescr: List<String>,
    val code: String,
    val codeContract: String,
    val codeProgramme: String,
    val codeScience: String,
    @SerialName("enddate") val endDate: String,
    val entityType: String,
    val name: String,
    val projectId: String,
    @SerialName("resaercherFullName") val researcherFullName: String,
    @SerialName("startdate") val startDate: String
)

@Serializable
data class ProjectDetails(
    val active: Boolean,
    val audiovisualSources: List<String>,
    val biblioRepresent: List<String>,
    val classificationCerif: List<String>,
    val classificationFord: List<String>,
    val classificationFrascati: List<String>,
    val code: String,
    val codeContract: String,
    val codeProgramme: String,
    val codeScience: String,
    val contact: Contact,
    val description: String,
    val enddate: String,
    val frame: String,
    val fteHoursDescription: String,
    val hasTender: Boolean,
    val id: Int,
    val name: String,
    val oldReports: List<String>,
    val organizations: List<Organization>,
    @SerialName("project") val projectInformation: ProjectInformation,
    val projectId: String,
    val reportMeta: List<String>,
    val resaercherFullName: String,
    val researchers: List<String>,
    val source: String,
    val startdate: String,
    val stat: String,
    val statadm: String,
    val statdate: String,
    val type: String
)

@Serializable
data class Contact(
    val addrid: Int,
    val conid: Int,
    val url: String
)

@Serializable
data class Organization(
    val frame: String,
    val id: Int,
    val stat: String,
    val statadm: String,
    val statdate: String,
    val type: String,
    val counter: String,
    val field: String,
    val science: String,
    val subfield: String,
    val city: String,
    val mstid: String,
    val name: String,
    val regnum: String,
    val remark: String,
    val rolecode: String,
    val sigla: String,
    val ssm: String,
    val statfrm: String,
    val taxnum: String
)

@Serializable
data class ProjectInformation(
    val frame: String,
    val id: Int,
    val stat: String,
    @SerialName("statadm") val statADM: String,
    @SerialName("statdate") val statDate: String,
    val type: String,
    @SerialName("enddate") val endDate: String,
    val mstidContract: String,
    @SerialName("mstidPrg") val mstidProgramme: String,
    val mstidScience: String,
    val startYear: Int,
    @SerialName("startdate") val startDate: String
)
