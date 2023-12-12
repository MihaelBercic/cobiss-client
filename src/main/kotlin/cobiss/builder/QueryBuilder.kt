package cobiss.builder

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
abstract class QueryBuilder<T, Q> where Q : QueryBuilder<T, Q> {
    protected var queryString = "*"
    protected var limit: QueryLimit = QueryLimit.All

    fun search(query: String): Q {
        if (query.isBlank()) throw Exception("Search query should not be empty.")
        this.queryString = query
        return this as Q
    }

    fun limit(quantity: QueryLimit): Q {
        if (quantity is QueryLimit.Some && quantity.quantity < 1) throw Exception("Quantity should be >= 1")
        this.limit = quantity
        return this as Q
    }

    abstract fun fetch(): List<T>
}