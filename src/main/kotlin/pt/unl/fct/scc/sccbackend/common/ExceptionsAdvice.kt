package pt.unl.fct.scc.sccbackend.common

import kotlinx.serialization.Serializable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.lang.Exception
import javax.servlet.http.HttpServletRequest

@RestControllerAdvice
class ExceptionsAdvice {

    @ExceptionHandler(value = [HttpException::class])
    fun handleHttpException(
        req: HttpServletRequest,
        ex: HttpException
    ): ResponseEntity<HttpError> {
        return buildHttpError(
            ex.localizedMessage,
            ex.status
        )
    }

    fun handleServerError(
        req: HttpServletRequest,
        ex: Exception
    ): ResponseEntity<HttpError> {
        return buildHttpError(
            "A fatal server error has occurred",
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

}

@Serializable
data class HttpError(
    val message: String,
    val status: Int
)

private fun buildHttpError(message: String, status: HttpStatus): ResponseEntity<HttpError> {
    val body = HttpError(message, status.value())
    return ResponseEntity.status(status)
        .body(body)
}