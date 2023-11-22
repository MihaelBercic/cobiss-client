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
