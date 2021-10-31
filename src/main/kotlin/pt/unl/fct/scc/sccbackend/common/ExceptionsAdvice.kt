package pt.unl.fct.scc.sccbackend.common

import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.NoHandlerFoundException
import javax.servlet.http.HttpServletRequest

@RestControllerAdvice
class ExceptionsAdvice {

    companion object {
        private val log = LoggerFactory.getLogger(ExceptionsAdvice::class.java)
    }

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

    @ExceptionHandler(value = [NoHandlerFoundException::class])
    fun handleNoHandlerFound(
        req: HttpServletRequest,
        ex: NoHandlerFoundException
    ): ResponseEntity<HttpError> {
        val status = HttpStatus.NOT_FOUND
        return buildHttpError(
            status.reasonPhrase,
            status
        )
    }

    @ExceptionHandler(value = [HttpMessageNotReadableException::class])
    fun handleMessageNotReadable(
        req: HttpServletRequest,
        ex: HttpMessageNotReadableException
    ): ResponseEntity<HttpError> {
        return buildHttpError(
            "The request is malformed",
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(value = [HttpRequestMethodNotSupportedException::class])
    fun handleMethodNotSupported(
        req: HttpServletRequest,
        ex: HttpRequestMethodNotSupportedException
    ): ResponseEntity<HttpError> {
        return buildHttpError(
            "Method not supported",
            HttpStatus.METHOD_NOT_ALLOWED
        )
    }

    @ExceptionHandler(value = [Exception::class])
    fun handleServerError(
        req: HttpServletRequest,
        ex: Exception
    ): ResponseEntity<HttpError> {
        log.error("A fatal exception has occurred", ex)

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
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
}