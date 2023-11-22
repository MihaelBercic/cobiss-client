package cobiss.builder


/**
 * @author Mihael Berčič on 22. 11. 23.
 */
abstract class QueryBuilder<T : CobissQuery> {

    protected var searchQuery = "*"
    protected var quantity: QueryLimit = QueryLimit.All
    protected var id: String = ""

    fun search(searchQuery: String): QueryBuilder<T> {
        this.searchQuery = searchQuery
        return this
    }

    fun limit(quantity: Int): QueryBuilder<T> {
        this.quantity = QueryLimit.Some(quantity)
        return this
    }

    fun id(id: String): QueryBuilder<T> {
        this.id = id
        return this
    }

    abstract fun build(): T
}
