package cobiss.builder.researcher

import kotlinx.serialization.Serializable

/**
 * @author Mihael Berčič on 23. 11. 23.
 */
@Serializable
data class Researcher(
    val frame: String = "",
    val id: Int = 0,
    val statadm: String = "",
    val statdate: String = "",
    val type: String = "",
    val classificationDescr: List<String> = emptyList(),
    val counter: String = "",
    val field: String = "",
    val science: String = "",
    val subfield: String = "",
    val allow: String = "",
    val firstName: String = "",
    val fullName: String = "",
    val lastName: String = "",
    val mstid: String = "",
    val rsrttl: String = "",
    val sex: String = "",
    val stat: String = "",
    val title: String = "",
    val typeDescription: String = ""
)

@Serializable
data class AccessData(
    val access: String = "",
    val checked: Boolean = false,
    val id: IdData = IdData(),
    val source: String = ""
)

@Serializable
data class IdData(
    val entity: String = "",
    val schemeid: Int = 0,
    val section: String = ""
)

@Serializable
data class ResearcherAccessRights(
    val showCerif: Boolean = false,
    val showContact: Boolean = false,
    val showEducation: Boolean = false,
    val showEmploy: Boolean = false,
    val showFrascati: Boolean = false,
    val showInfo: Boolean = false,
    val showKeyws: Boolean = false,
    val showLangSkls: Boolean = false,
    val showMentorship: Boolean = false,
    val showOldEmploy: Boolean = false,
    val showOrcid: Boolean = false,
    val showRepBiblio: Boolean = false,
    val showVideo: Boolean = false
)

@Serializable
data class IdDataWithWeight(
    val field: String = "",
    val science: Int = 0,
    val subfield: String = "",
    val weight: Int = 0,
    val sraId: IdData = IdData()
)

@Serializable
data class ClassificationFrascati(
    val field: String = "",
    val science: Int = 0,
    val subfield: String = "",
    val weight: Int = 0,
    val sraId: IdData = IdData()
)

@Serializable
data class ContactData(
    val addrid: Int = 0,
    val conid: Int = 0,
    val email: String = "",
    val emailPostfix: String = "",
    val emailPrefix: String = ""
)

@Serializable
data class EducationData(
    val countrycode: String = "",
    val degree: String = "",
    val faculty: String = "",
    val id: IdDataWithWeight = IdDataWithWeight(),
    val lvlcode: String = "",
    val university: String = "",
    val year: String = ""
)

@Serializable
data class EmployData(
    val frame: String = "",
    val id: Int = 0,
    val stat: String = "",
    val statadm: String = "",
    val statdate: String = "",
    val type: String = "",
    val code: String = "",
    val empltype: String = "",
    val name: String = "",
    val notAllowed: Boolean = false,
    val orgCode: String = "",
    val orgName: String = "",
    val orgid: Int = 0,
    val resaercherFullName: String = "",
    val researchload: Double = 0.0,
    val rolecode: String = "",
    val rsrload: Double = 0.0,
    val rsrttlby: String = "",
    val rsrttldate: String = ""
)

@Serializable
data class ResearcherDetails(
    val accessRights: ResearcherAccessRights = ResearcherAccessRights(),
    val allow: String = "",
    val audiovisualSources: List<String> = emptyList(),
    val biblioRepresent: List<String> = emptyList(),
    val classificationCerif: List<ClassificationCerif> = emptyList(),
    val classificationFrascati: List<ClassificationFrascati> = emptyList(),
    val contact: ContactData = ContactData(),
    val educations: List<EducationData> = emptyList(),
    val employs: List<EmployData> = emptyList(),
    val firstName: String = "",
    val frame: String = "",
    val fullName: String = "",
    val hasTender: Boolean = false,
    val id: Int = 0,
    val internacionalprojects: List<String> = emptyList(),
    val isLeader: Boolean = false,
    val langSkills: List<LangSkillData> = emptyList(),
    val lastName: String = "",
    val mstid: String = "",
    val previousEmployments: List<EmployData> = emptyList(),
    val programmesAndProjectsResults: List<ProgramResultData> = emptyList(),
    val programs: List<String> = emptyList(),
    val projects: List<ProjectData> = emptyList(),
    val researcher: Researcher = Researcher(),
    val rsrttl: String = "",
    val sex: String = "",
    val stat: String = "",
    val statadm: String = "",
    val statdate: String = "",
    val title: String = "",
    val type: String = "",
    val yngResearchers: List<String> = emptyList()
)

@Serializable
data class ClassificationCerif(
    val field: String = "",
    val part: Int = 0,
    val science: String = "",
    val weight: Int = 0,
    val cerifId: CerifIdData = CerifIdData()
)

@Serializable
data class CerifIdData(
    val id: Int = 0,
    val weight: Int = 0
)


@Serializable
data class LangSkillData(
    val id: LangSkillIdData = LangSkillIdData(),
    val lngcode: String = "",
    val read: String = "",
    val speak: String = "",
    val writte: String = ""
)

@Serializable
data class LangSkillIdData(
    val id: Int = 0,
    val languageCode: String = ""
)

@Serializable
data class ProgramResultData(
    val pk: PkData = PkData()
)

@Serializable
data class PkData(
    val cobissid: String = "",
    val signific: String = ""
)

@Serializable
data class ProjectData(
    val frame: String = "",
    val id: Int = 0,
    val stat: String = "",
    val statadm: String = "",
    val statdate: String = "",
    val type: String = "",
    val counter: String = "",
    val field: String = "",
    val science: String = "",
    val subfield: String = "",
    val avfte: Int = 0,
    val code: String = "",
    val codeContract: String = "",
    val codeProgramme: String = "",
    val codeScience: String = "",
    val enddate: String = "",
    val entityType: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val mstrank: String = "",
    val name: String = "",
    val projectId: String = "",
    val resaercherFullName: String = "",
    val rsrCode: String = "",
    val rsrid: Int = 0,
    val rsrttl: String = "",
    val startdate: String = "",
    val uplimit: Int = 0
)
