package cobiss.builder

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
sealed class QueryLimit(val representation: String) {
    data class Some(val quantity: Int) : QueryLimit("$quantity")
    data object All : QueryLimit("ALL")
}