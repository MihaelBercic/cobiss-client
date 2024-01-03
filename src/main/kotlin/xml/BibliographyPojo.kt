package xml

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root
import org.simpleframework.xml.Text

/**
 * @author Mihael Berčič on 19. 12. 23.
 */
@Root(name = "Bibliography", strict = false)
class BibliographyPojo {

    @field:Attribute(name = "biblioType", required = false)
    var type: String? = null

    @field:Attribute(name = "code", required = false)
    var code: String? = null

    @field:Element(name = "Name")
    var name: String? = null

    @field:ElementList(required = false, name = "BiblioDiv")
    var divisions: List<BibliographyDivision> = mutableListOf()

}

@Root(name = "BiblioDiv", strict = false)
class BibliographyDivision {

    @field:Attribute(name = "level", required = false)
    var level: String = ""

    @field:Attribute(name = "id", required = false)
    var id: String = ""

    @field:Element(name = "Title", required = false)
    var title: String = ""

    @field:ElementList(required = false, inline = true, entry = "BiblioDiv")
    var divisions: List<BibliographyDivision> = mutableListOf()

    @field:Element(required = false, name = "BiblioList")
    var entryList: BibliographyEntryList? = null

}

@Root(name = "BiblioList", strict = false)
class BibliographyEntryList {
    @field:ElementList(required = false, name = "BiblioEntry", inline = true)
    var entries: List<BibliographyEntry> = mutableListOf()
}

@Root(name = "BiblioEntry", strict = false)
class BibliographyEntry {
    @field:Attribute(required = false, name = "bno")
    var bno: String? = null

    @field:Attribute(required = false, name = "outBno")
    var outBno: String? = null

    @field:Attribute(required = false, name = "type")
    var type: String? = null

    @field:Element(name = "Title")
    var title: String = ""

    @field:Element(required = false, name = "AuthorGroup")
    var authorsGroup: AuthorGroup? = null

    @field:ElementList(required = false, name = "BiblioSet", inline = true)
    var bibSet: List<BibliographySet>? = null

    @field:Element(required = false, name = "PubYear")
    var publicationYear: Int? = null

    @field:Element(required = false, name = "Identifier")
    var identifier: Identifier? = null

    @field:Element(required = true, name = "Typology")
    var typology: Typology? = null

    @field:Element(required = false, name = "Evaluation")
    var evaluation: Evaluation? = null
}

@Root(name = "BiblioSet", strict = false)
class BibliographySet {
    @field:Attribute(name = "relation", required = false)
    var relation: String = ""

    @field:Element(name = "Title", required = false)
    var title: String = ""


    @field:Element(name = "ISSN", required = false)
    var issn: String = ""
}

data class Typology(
    @field:Attribute(name = "code")
    var code: String? = null
)

data class Identifier(
    @field:Element(name = "DOI")
    var doi: String? = null
)

@Root(name = "AuthorGroup", strict = false)
data class AuthorGroup(
    @field:ElementList(inline = true, required = false)
    var authors: List<Author>? = null
)

@Root(name = "Author", strict = false)
data class Author(
    @field:Attribute(name = "responsibility")
    var responsibility: String? = null,

    @field:Element(name = "FirstName")
    var firstName: String? = null,

    @field:Element(name = "LastName")
    var lastName: String? = null,

    @field:Element(name = "Dates", required = false)
    var dates: String? = null,

    @field:Element(name = "CodeRes", required = false)
    var codeRes: String? = null,

    @field:Element(name = "Contrib", required = false)
    var contrib: String? = null,

    @field:Element(name = "CONOR", required = false)
    var conor: Conor? = null
)

@Root(name = "CONOR")
data class Conor(
    @field:Attribute
    var system: String? = null,

    @field:Attribute
    var id: String? = null
)

@Root(name = "Evaluation", strict = false)
data class Evaluation(
    @field:Element(name = "Category", required = false)
    var category: String? = null,

    @field:Element(name = "Performance", required = false)
    var performance: String? = null,

    @field:Element(name = "HighPerformance", required = false)
    var highPerformance: String? = null,

    @field:Element(name = "Base", required = false)
    var base: String? = null,

    @field:Element(name = "BasesMbp", required = false)
    var basesMbp: String? = null,

    @field:Element(name = "Osic", required = false)
    var osic: String? = null,

    @field:Element(name = "Points", required = false)
    var points: String? = null,

    @field:Element(name = "AuthorsNo", required = false)
    var authorsNo: String? = null
)
