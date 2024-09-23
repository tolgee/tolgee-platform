package io.tolgee

import io.sentry.Sentry
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.tolgee.constants.Message
import io.tolgee.dtos.request.validators.ValidationErrorType
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ErrorException
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.exceptions.ErrorResponseTyped
import io.tolgee.exceptions.NotFoundException
import io.tolgee.security.ratelimit.RateLimitResponseBody
import io.tolgee.security.ratelimit.RateLimitedException
import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.apache.catalina.connector.ClientAbortException
import org.apache.commons.lang3.exception.ExceptionUtils
import org.hibernate.QueryException
import org.slf4j.LoggerFactory
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.transaction.TransactionSystemException
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.io.Serializable
import java.util.*
import java.util.function.Consumer

@RestControllerAdvice
class ExceptionHandlers {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationExceptions(
    ex: MethodArgumentNotValidException,
  ): ResponseEntity<Map<String, Map<String, String>>> {
    val errors: MutableMap<String, String> = HashMap()
    ex.bindingResult.allErrors.forEach(
      Consumer { error: ObjectError ->
        val fieldName = (error as FieldError).field
        val errorMessage = error.getDefaultMessage()
        errors[fieldName] = errorMessage ?: ""
      },
    )
    return ResponseEntity(
      Collections.singletonMap<String, Map<String, String>>(ValidationErrorType.STANDARD_VALIDATION.name, errors),
      HttpStatus.BAD_REQUEST,
    )
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleValidationExceptions(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponseBody> {
    return ResponseEntity(
      ErrorResponseBody(Message.WRONG_PARAM_TYPE.code, listOf(ex.parameter.parameterName) as List<Serializable>?),
      HttpStatus.BAD_REQUEST,
    )
  }

  @ExceptionHandler(ValidationException::class)
  fun handleCustomValidationExceptions(
    ex: ValidationException,
  ): ResponseEntity<Map<String, Map<String, List<String>>>> {
    val errors: MutableMap<String, List<String>> = HashMap()
    for (validationError in ex.validationErrors) {
      errors[validationError.message.code] = Arrays.asList(*validationError.parameters)
    }
    return ResponseEntity(
      Collections.singletonMap<String, Map<String, List<String>>>(ValidationErrorType.CUSTOM_VALIDATION.name, errors),
      HttpStatus.BAD_REQUEST,
    )
  }

  @ExceptionHandler(BindException::class)
  fun handleBindExceptions(ex: BindException): ResponseEntity<MutableMap<String, Map<String, String>>> {
    val errors: MutableMap<String, String> = HashMap()

    ex.bindingResult.allErrors.forEach { error: ObjectError ->
      val fieldName = (error as FieldError).field
      val errorMessage = error.getDefaultMessage()
      errors[fieldName] = errorMessage ?: ""
    }

    return ResponseEntity(
      Collections.singletonMap(ValidationErrorType.STANDARD_VALIDATION.name, errors),
      HttpStatus.BAD_REQUEST,
    )
  }

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleMissingServletRequestParameterException(
    ex: MissingServletRequestParameterException,
  ): ResponseEntity<Map<String, Map<String, String?>>> {
    val errors = Collections.singletonMap(ex.parameterName, ex.message)
    return ResponseEntity(
      Collections.singletonMap(ValidationErrorType.STANDARD_VALIDATION.name, errors),
      HttpStatus.BAD_REQUEST,
    )
  }

  @ExceptionHandler(MissingServletRequestPartException::class)
  fun handleMissingServletRequestPartException(
    ex: MissingServletRequestPartException,
  ): ResponseEntity<Map<String, Map<String, String?>>> {
    val errors = Collections.singletonMap(ex.requestPartName, ex.message)
    return ResponseEntity(
      Collections.singletonMap(ValidationErrorType.STANDARD_VALIDATION.name, errors),
      HttpStatus.BAD_REQUEST,
    )
  }

  @ApiResponse(
    responseCode = "400",
    content = [
      Content(
        mediaType = "application/json",
        schema =
          Schema(
            oneOf = [ErrorResponseTyped::class, ErrorResponseBody::class],
            example = """{"code": "you_did_something_wrong", "params": ["something", "wrong"]}""",
          ),
      ),
    ],
  )
  @ApiResponse(
    responseCode = "403",
    content = [
      Content(
        mediaType = "application/json",
        schema =
          Schema(
            oneOf = [ErrorResponseTyped::class, ErrorResponseBody::class],
            example = """{"code": "operation_not_permitted", "params": ["translations.edit"]}""",
          ),
      ),
    ],
  )
  @ApiResponse(
    responseCode = "401",
    content = [
      Content(
        mediaType = "application/json",
        schema =
          Schema(
            oneOf = [ErrorResponseTyped::class, ErrorResponseBody::class],
            example = """{"code": "unauthenticated"}""",
          ),
      ),
    ],
  )
  @ExceptionHandler(ErrorException::class)
  fun handleServerError(ex: ErrorException): ResponseEntity<ErrorResponseBody> {
    return ResponseEntity(ex.errorResponseBody, ex.httpStatus)
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleServerError(ex: EntityNotFoundException?): ResponseEntity<ErrorResponseBody> {
    logger.debug("Entity not found", ex)
    return ResponseEntity(ErrorResponseBody(Message.RESOURCE_NOT_FOUND.code, null), HttpStatus.NOT_FOUND)
  }

  @ApiResponse(
    responseCode = "404",
    content = [
      Content(
        mediaType = "application/json",
        schema =
          Schema(
            oneOf = [ErrorResponseTyped::class, ErrorResponseBody::class],
            example = """{"code": "resource_not_found", "params": null}""",
          ),
      ),
    ],
  )
  @ExceptionHandler(NotFoundException::class)
  fun handleNotFound(ex: NotFoundException): ResponseEntity<ErrorResponseBody> {
    logger.debug(ex.message, ex)
    return ResponseEntity(ErrorResponseBody(ex.msg.code, null), HttpStatus.NOT_FOUND)
  }

  @ExceptionHandler(MaxUploadSizeExceededException::class)
  fun handleFileSizeLimitExceeded(ex: MaxUploadSizeExceededException): ResponseEntity<ErrorResponseBody> {
    return ResponseEntity(
      ErrorResponseBody(Message.FILE_TOO_BIG.code, listOf()),
      HttpStatus.BAD_REQUEST,
    )
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponseBody> {
    val params = ex.rootCause?.message?.let { listOf(it) }
    return ResponseEntity(
      ErrorResponseBody(Message.REQUEST_PARSE_ERROR.code, params),
      HttpStatus.BAD_REQUEST,
    )
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
  fun handleFileSizeLimitExceeded(ex: HttpRequestMethodNotSupportedException): ResponseEntity<Void> {
    return ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED)
  }

  @ExceptionHandler(InvalidDataAccessApiUsageException::class)
  fun handleFileSizeLimitExceeded(ex: InvalidDataAccessApiUsageException): ResponseEntity<ErrorResponseBody> {
    Sentry.captureException(ex)
    val contains = ex.message?.contains("could not resolve property", true) ?: false
    if (contains) {
      return ResponseEntity(
        ErrorResponseBody(Message.UNKNOWN_SORT_PROPERTY.code, null),
        HttpStatus.BAD_REQUEST,
      )
    }
    throw ex
  }

  @ExceptionHandler(QueryException::class)
  fun handleQueryException(ex: QueryException): ResponseEntity<ErrorResponseBody> {
    if (ex.message!!.contains("could not resolve property")) {
      return handleServerError(BadRequestException(Message.COULD_NOT_RESOLVE_PROPERTY))
    }
    throw ex
  }

  @ExceptionHandler(RateLimitedException::class)
  fun handleRateLimited(ex: RateLimitedException): ResponseEntity<RateLimitResponseBody> {
    return ResponseEntity(
      RateLimitResponseBody(Message.RATE_LIMITED, ex.retryAfter, ex.global),
      HttpStatus.TOO_MANY_REQUESTS,
    )
  }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFound(ex: NoResourceFoundException): ResponseEntity<ErrorResponseBody> {
    logger.debug("No resource found", ex)
    return ResponseEntity(
      ErrorResponseBody(Message.RESOURCE_NOT_FOUND.code, listOf(ex.resourcePath)),
      HttpStatus.NOT_FOUND,
    )
  }

  @ExceptionHandler(Throwable::class)
  fun handleOtherExceptions(ex: Throwable): ResponseEntity<ErrorResponseBody> {
    Sentry.captureException(ex)
    logger.error(ex.stackTraceToString())
    return ResponseEntity(
      ErrorResponseBody(
        "unexpected_error_occurred",
        listOf(ex::class.java.name),
      ),
      HttpStatus.INTERNAL_SERVER_ERROR,
    )
  }

  @ExceptionHandler
  fun handleTransactionExceptions(exception: TransactionSystemException): ResponseEntity<ErrorResponseBody> {
    val rootCause = ExceptionUtils.getRootCause(exception)
    if (rootCause is NotFoundException) {
      return handleNotFound(rootCause)
    }
    if (rootCause is BadRequestException) {
      return handleServerError(rootCause)
    }
    throw exception
  }

  @ExceptionHandler(ClientAbortException::class)
  fun handleClientAbortException(
    exception: ClientAbortException?,
    request: HttpServletRequest,
  ) {
    val message = "ClientAbortException generated by request {} {} from remote address {} with X-FORWARDED-FOR {}"
    val headerXFF = request.getHeader("X-FORWARDED-FOR")
    logger.warn(message, request.method, request.requestURL, request.remoteAddr, headerXFF)
  }
}
