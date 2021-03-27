package io.tolgee

import io.tolgee.constants.Message
import io.tolgee.dtos.request.validators.ValidationErrorType
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.ErrorException
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.exceptions.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import java.util.*
import java.util.function.Consumer
import javax.persistence.EntityNotFoundException

@RestControllerAdvice
class ExceptionHandlers {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Map<String, String>>> {
        val errors: MutableMap<String, String> = HashMap()
        ex.bindingResult.allErrors.forEach(Consumer { error: ObjectError ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.getDefaultMessage()
            errors[fieldName] = errorMessage ?: ""
        })
        return ResponseEntity(Collections.singletonMap<String, Map<String, String>>(ValidationErrorType.STANDARD_VALIDATION.name, errors), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ValidationException::class)
    fun handleCustomValidationExceptions(ex: ValidationException): ResponseEntity<Map<String, Map<String, List<String>>>> {
        val errors: MutableMap<String, List<String>> = HashMap()
        for (validationError in ex.validationErrors) {
            errors[validationError.message.code] = Arrays.asList(*validationError.parameters)
        }
        return ResponseEntity(Collections.singletonMap<String, Map<String, List<String>>>(ValidationErrorType.CUSTOM_VALIDATION.name, errors), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleCMissingServletRequestParameterException(ex: MissingServletRequestParameterException): ResponseEntity<Map<String, Map<String, String?>>> {
        val errors = Collections.singletonMap(ex.parameterName, ex.message)
        return ResponseEntity(Collections.singletonMap(ValidationErrorType.STANDARD_VALIDATION.name, errors), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ErrorException::class)
    fun handleServerError(ex: ErrorException): ResponseEntity<ErrorResponseBody> {
        return ResponseEntity(ex.errorResponseBody, ex.httpStatus)
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleServerError(ex: EntityNotFoundException?): ResponseEntity<ErrorResponseBody> {
        return ResponseEntity(ErrorResponseBody(Message.RESOURCE_NOT_FOUND.code, null), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ErrorResponseBody> {
        return ResponseEntity(ErrorResponseBody(ex.msg!!.code, null), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleFileSizeLimitExceeded(ex: MaxUploadSizeExceededException): ResponseEntity<ErrorResponseBody> {
        return ResponseEntity(ErrorResponseBody(Message.FILE_TOO_BIG.code, listOf()),
                HttpStatus.BAD_REQUEST)
    }
}
