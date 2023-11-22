package cobiss.builder

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
abstract class QueryBuilder<T> {
    protected var queryString = "*"
    protected var limit: QueryLimit = QueryLimit.All

    protected fun checkAndSetSearchQuery(query: String) {
        if (query.isBlank()) throw Exception("Search query should not be empty.")
        this.queryString = query
    }

    protected fun checkAndSetLimit(quantity: Int) {
        if (quantity < 1) throw Exception("Quantity should be >= 1")
        this.limit = QueryLimit.Some(quantity)
    }

    abstract fun search(query: String): QueryBuilder<T>
    abstract fun limit(quantity: Int): QueryBuilder<T>

    abstract fun fetch(): List<T>
}