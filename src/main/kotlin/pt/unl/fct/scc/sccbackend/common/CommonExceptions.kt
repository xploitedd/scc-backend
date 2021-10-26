package pt.unl.fct.scc.sccbackend.common

import org.springframework.http.HttpStatus
import java.lang.RuntimeException

sealed class HttpException(
    message: String,
    val status: HttpStatus
): RuntimeException(message)

sealed class BadRequestException(message: String) : HttpException(message, HttpStatus.BAD_REQUEST)
class UnauthorizedException : HttpException("Invalid credentials", HttpStatus.UNAUTHORIZED)
class ForbiddenException : HttpException("You do not have access to this resource", HttpStatus.FORBIDDEN)