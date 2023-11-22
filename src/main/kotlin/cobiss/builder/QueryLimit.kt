package cobiss.builder

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
sealed class QueryLimit(val representation: String) {
    data class Some(val value: Int) : QueryLimit("$value")
    data object All : QueryLimit("ALL")
}