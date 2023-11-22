package cobiss.builder.project

import cobiss.builder.QueryBuilder

/**
 * @author Mihael Berčič on 22. 11. 23.
 */
class ProjectsQueryBuilder : QueryBuilder<ProjectQuery>() {

    override fun build(): ProjectQuery = ProjectQuery(
        endpoint = "project",
        searchQuery = searchQuery,
        limit = quantity,
        id = id
    )
}