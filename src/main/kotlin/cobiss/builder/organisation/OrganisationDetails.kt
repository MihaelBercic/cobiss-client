package cobiss.builder.organisation

import kotlinx.serialization.Serializable

/**
 * @author Mihael Berčič on 23. 11. 23.
 */
@Serializable
data class OrganisationDetails(
    val frame: String,
    val id: Int,
    val stat: String? = null,
    val statAdn: String? = null,
    val statDate: String? = null,
    val acccat: String? = null,
    val accNumber: String? = null,
    val type: String,
    val counter: Int,
    val field: String? = null,
    val science: String? = null,
    val subfield: String? = null,
    val city: String? = null,
    val mstid: String,
    val name: String,
    val regnum: String? = null,
    val statFrm: String? = null,
    val taxNumber: String? = null,
    val sigla: String? = null,

    )