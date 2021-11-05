package pt.unl.fct.scc.sccbackend.common

import org.springframework.http.HttpStatus

sealed class HttpException(
    message: String,
    val status: HttpStatus
): RuntimeException(message)

class BadRequestException(message: String) : HttpException(message, HttpStatus.BAD_REQUEST)
class UnauthorizedException : HttpException("Invalid credentials", HttpStatus.UNAUTHORIZED)
class ForbiddenException : HttpException("You do not have access to this resource", HttpStatus.FORBIDDEN)
class ConflictException(conflict: String) : HttpException(conflict, HttpStatus.CONFLICT)
class NotFoundException : HttpException("The specified resource wasn't found", HttpStatus.NOT_FOUND)