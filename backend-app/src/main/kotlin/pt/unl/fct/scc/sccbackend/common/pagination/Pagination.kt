package pt.unl.fct.scc.sccbackend.common.pagination

annotation class PaginationDefaults(val page: Int, val limit: Int)

data class Pagination(
    val page: Int,
    val limit: Int
) {
    val offset = page * limit
}