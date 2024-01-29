package xml

/**
 * @author Mihael Berčič on 29. 01. 24.
 */
enum class BibliographyOutputFormat(val abbreviation: Char, val extension: String) {
    Html('H', "html"), Xml('X', "xml")
}