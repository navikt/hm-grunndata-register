package no.nav.hm.grunndata.register.error

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.micronaut.context.annotation.Replaces
import io.micronaut.core.convert.exceptions.ConversionErrorException
import io.micronaut.http.*
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ConversionErrorHandler
import io.micronaut.http.server.exceptions.ExceptionHandler
import io.micronaut.http.server.exceptions.JsonExceptionHandler
import org.slf4j.LoggerFactory
import java.util.*
import jakarta.inject.Singleton

@Produces
@Singleton
class BadRequestErrorHandler : ExceptionHandler<BadRequestException, HttpResponse<ErrorMessage>> {

    override fun handle(request: HttpRequest<*>, error: BadRequestException): HttpResponse<ErrorMessage> {
        val response =  when (error.type) {
            ErrorType.NOT_FOUND -> HttpResponse.notFound(createMessage(error))
            ErrorType.MISSING_PARAMETER, ErrorType.INVALID_VALUE, ErrorType.PARSE_ERROR -> HttpResponse.badRequest(createMessage(error))
            ErrorType.CONFLICT -> HttpResponseFactory.INSTANCE.status<ErrorMessage>(HttpStatus.CONFLICT).body(createMessage(error))
            ErrorType.UNKNOWN -> HttpResponse.serverError(createMessage(error))
        }
        if (error.type != ErrorType.NOT_FOUND) { LOG.error(response.body().toString()) }
        return response
    }
    private fun createMessage(error: BadRequestException) = ErrorMessage(message = error.message!!, errorType = error.type)
}

@Produces
@Singleton
@Replaces(ConversionErrorHandler::class)
class ConversionExceptionHandler : ExceptionHandler<ConversionErrorException, HttpResponse<ErrorMessage>> {
    override fun handle(request: HttpRequest<*>?, error: ConversionErrorException): HttpResponse<ErrorMessage> {
        val response = when (error.cause) {
            is JsonProcessingException -> handleJsonProcessingException(error.cause as JsonProcessingException)
            else -> HttpResponse.serverError(ErrorMessage(error.message!!, ErrorType.UNKNOWN))
        }
        LOG.error(response.body().toString())
        return response
    }
}

@Produces
@Singleton
@Replaces(JsonExceptionHandler::class)
class ApiJsonErrorHandler : ExceptionHandler<JsonProcessingException, HttpResponse<ErrorMessage>> {

    override fun handle(request: HttpRequest<*>?, error: JsonProcessingException): HttpResponse<ErrorMessage> {
        val response = handleJsonProcessingException(error)
        LOG.error(response.body().toString())
        return response
    }

}

private fun handleJsonProcessingException(error: JsonProcessingException): HttpResponse<ErrorMessage> {
    return when (error) {
        is JsonParseException -> HttpResponse
                .badRequest(ErrorMessage("Parse error: at ${error.location}", ErrorType.PARSE_ERROR))
        is MissingKotlinParameterException -> HttpResponse
                .badRequest(ErrorMessage("Missing parameter: ${error.parameter.name}", ErrorType.MISSING_PARAMETER))
        is InvalidFormatException -> HttpResponse
                .badRequest(ErrorMessage("Invalid value: ${error.value} at ${error.pathReference}",
                    ErrorType.INVALID_VALUE
                ))
        is ValueInstantiationException -> HttpResponse.badRequest(ErrorMessage("Wrong value: ${error.message} at ${error.pathReference}",
            ErrorType.INVALID_VALUE
        ))
        else -> HttpResponse.badRequest(ErrorMessage("Bad Json: ${error.localizedMessage}", ErrorType.UNKNOWN))
    }
}

enum class ErrorType {
    PARSE_ERROR, MISSING_PARAMETER, INVALID_VALUE, CONFLICT, NOT_FOUND, UNKNOWN
}

// Global error logger for errorhandler
private val LOG = LoggerFactory.getLogger("HttpRequestErrorHandler")

class BadRequestException(message: String, val type: ErrorType = ErrorType.INVALID_VALUE) : Throwable(message)

data class ErrorMessage (val message : String, val errorType: ErrorType, val errorRef: UUID = UUID.randomUUID())

