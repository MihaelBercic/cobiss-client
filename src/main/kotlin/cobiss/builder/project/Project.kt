package cobiss.builder.project

import cobiss.builder.researcher.Researcher
import cobiss.builder.researcher.ResearcherDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
@Serializable
data class Project(
    val frame: String? = null,
    val id: Int? = null,
    val stat: String? = null,
    val type: String? = null,
    val classificationDescr: List<String>? = null,
    val code: String? = null,
    val codeContract: String? = null,
    val codeProgramme: String? = null,
    val codeScience: String? = null,
    val entityType: String? = null,
    val name: String? = null,
    val projectId: String? = null,
    val mstidContract: String? = null,
    val mstidScience: String? = null,
    val startYear: Int? = null,
    @SerialName("statadm") val statADM: String? = null,
    @SerialName("statdate") val statDate: String? = null,
    @SerialName("enddate") val endDate: String? = null,
    @SerialName("resaercherFullName") val researcherFullName: String? = null,
    @SerialName("startdate") val startDate: String? = null,
    @SerialName("mstidPrg") val mstidProgramme: String? = null,
)

@Serializable
data class ProjectDetails(
    val active: Boolean,
    val code: String,
    val codeContract: String,
    val codeProgramme: String,
    val codeScience: String,
    val description: String = "",
    val enddate: String,
    val frame: String,
    val fteHoursDescription: String,
    val hasTender: Boolean,
    val id: Int,
    val name: String,
    // val oldReports: List<String>,
    val organizations: List<Organization>,
    @SerialName("project") val projectInformation: Project,
    val projectId: String,
    // val reportMeta: List<String>,
    val resaercherFullName: String,
    val researchers: List<Researcher>,
    val source: String,
    val startdate: String,
    val stat: String,
    val statadm: String,
    val statdate: String,
    val type: String,
    // val audiovisualSources: List<String> = emptyList(),
    // val biblioRepresent: List<String> = emptyList(),
    // val classificationCerif: List<String> = emptyList(),
    // val classificationFord: List<String> = emptyList(),
    // val classificationFrascati: List<String> = emptyList(),
)

@Serializable
data class Contact(
    val addrid: Int? = null,
    val conid: Int? = null,
    val url: String? = null
)

@Serializable
data class Organization(
    val frame: String? = null,
    val id: Int? = null,
    val stat: String? = null,
    val statadm: String? = null,
    val statdate: String? = null,
    val type: String? = null,
    val counter: String? = null,
    val field: String? = null,
    val science: String? = null,
    val subfield: String? = null,
    val city: String? = null,
    val mstid: String? = null,
    val name: String? = null,
    val regnum: String? = null,
    val remark: String? = null,
    val rolecode: String? = null,
    val sigla: String? = null,
    val ssm: String? = null,
    val statfrm: String? = null,
    val taxnum: String? = null
)