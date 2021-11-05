package pt.unl.fct.scc.sccbackend.common.accessControl

import kotlinx.coroutines.runBlocking
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import pt.unl.fct.scc.sccbackend.common.UnauthorizedException
import pt.unl.fct.scc.sccbackend.common.accessControl.repo.AccessControlRepo
import pt.unl.fct.scc.sccbackend.users.model.User
import java.util.*

@Service
class UserResolver(
    val repo: AccessControlRepo,
    val passwordEncoder: PasswordEncoder
) : HandlerMethodArgumentResolver {

    companion object {
        private const val BASIC_TYPE = "Basic"
        private val base64Decoder = Base64.getUrlDecoder()
    }

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.parameterType == User::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): User? {
        try {
            val authorization = webRequest.getHeader(HttpHeaders.AUTHORIZATION)
                ?: throw UnauthorizedException()

            val parts = authorization.split(" ")
            if (parts.size < 2 || !parts[0].equals(BASIC_TYPE, ignoreCase = true))
                throw UnauthorizedException()

            val authToken = base64Decoder.decode(parts[1])
                .decodeToString()

            val authTokenParts = authToken.split(":", limit = 2)
            if (authTokenParts.size < 2)
                throw UnauthorizedException()

            val username = authTokenParts[0]
            val password = authTokenParts[1]

            return runBlocking {
                getUser(username, password)
            }
        } catch (ex: UnauthorizedException) {
            if (parameter.isOptional)
                return null

            throw ex
        }
    }

    private suspend fun getUser(username: String, password: String): User {
        val user = repo.getUserByUsername(username)
            ?: throw UnauthorizedException()

        if (!passwordEncoder.matches(password, user.password))
            throw UnauthorizedException()

        return user
    }

}