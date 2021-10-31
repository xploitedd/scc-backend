package pt.unl.fct.scc.sccbackend.common.pagination

import org.springframework.core.MethodParameter
import org.springframework.stereotype.Service
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

private const val PAGE_PARAMETER = "page"
private const val LIMIT_PARAMETER = "limit"

private data class Defaults(val page: Int, val limit: Int)

private fun PaginationDefaults.toDefaults() = Defaults(page, limit)

private val DEFAULTS = Defaults(0, 10)

@Service
class PaginationResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.parameterType == Pagination::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Pagination {
        val defaults = parameter.getParameterAnnotation(PaginationDefaults::class.java)?.toDefaults()
            ?: DEFAULTS

        val page = webRequest.getParameter(PAGE_PARAMETER)?.toIntOrNull()
            ?: defaults.page

        val limit = webRequest.getParameter(LIMIT_PARAMETER)?.toIntOrNull()
            ?: defaults.limit

        return Pagination(page, limit)
    }

}