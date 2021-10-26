package pt.unl.fct.scc.sccbackend.common.argumentResolvers

import kotlinx.coroutines.runBlocking
import org.litote.kmongo.eq
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import pt.unl.fct.scc.sccbackend.common.UnauthorizedException
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.users.model.User
import java.util.*

@Service
class UserResolver(
    val tm: KMongoTM,
    val passwordEncoder: PasswordEncoder
) : HandlerMethodArgumentResolver {

    companion object {
        private const val BASIC_TYPE = "Basic"

        private val log = LoggerFactory.getLogger(UserResolver::class.java)
        private val base64Decoder = Base64.getUrlDecoder()

        const val USERNAME_ATTR = "username"
        const val PASSWORD_ATTR = "password"
    }

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.parameterType == User::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): User? {
        // TODO: add support for optional kotlin parameters
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

        val nickname = authTokenParts[0]
        val password = authTokenParts[1]

        return runBlocking {
            getUser(nickname, password)
        }
    }

    private suspend fun getUser(nickname: String, password: String): User {
        // TODO: improve this. maybe move to a repo class?
        val col = tm.database.getCollection<User>()
        val user = col.findOne(User::nickname eq nickname)
            ?: throw UnauthorizedException()

        if (!passwordEncoder.matches(password, user.password))
            throw UnauthorizedException()

        return user
    }

}