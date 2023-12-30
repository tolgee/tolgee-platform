package io.tolgee.component

import io.tolgee.constants.Message
import io.tolgee.dtos.request.LanguageDto
import io.tolgee.dtos.request.validators.ValidationError
import io.tolgee.dtos.request.validators.ValidationErrorType
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.service.LanguageService
import org.springframework.stereotype.Component
import java.util.*

@Component
class LanguageValidator(
  private val languageService: LanguageService,
) {
  fun validateEdit(
    id: Long,
    dto: LanguageDto,
  ) {
    val validationErrors = LinkedHashSet<ValidationError>()

    // handle edit validation
    val language = languageService.findById(id).orElseThrow({ NotFoundException() })
    val project = language.project
    if (language.name != dto.name) {
      validateNameUniqueness(dto, project).ifPresent { e: ValidationError -> validationErrors.add(e) }
    }
    if (language.tag != dto.tag) {
      validateTagUniqueness(dto, project).ifPresent { e: ValidationError -> validationErrors.add(e) }
    }
    if (!validationErrors.isEmpty()) {
      throw ValidationException(validationErrors)
    }
  }

  fun validateCreate(
    dto: LanguageDto,
    project: Project?,
  ) {
    val validationErrors = LinkedHashSet<ValidationError>()

    // handle create validation
    validateTagUniqueness(dto, project).ifPresent { e: ValidationError -> validationErrors.add(e) }
    validateNameUniqueness(dto, project).ifPresent { e: ValidationError -> validationErrors.add(e) }
    if (!validationErrors.isEmpty()) {
      throw ValidationException(validationErrors)
    }
  }

  private fun validateNameUniqueness(
    dto: LanguageDto,
    project: Project?,
  ): Optional<ValidationError> {
    return if (languageService.findByName(dto.name, project!!).isPresent) {
      Optional.of(ValidationError(ValidationErrorType.CUSTOM_VALIDATION, Message.LANGUAGE_NAME_EXISTS))
    } else {
      Optional.empty()
    }
  }

  private fun validateTagUniqueness(
    dto: LanguageDto,
    project: Project?,
  ): Optional<ValidationError> {
    return if (languageService.findByTag(dto.tag, project!!).isPresent) {
      Optional.of(ValidationError(ValidationErrorType.CUSTOM_VALIDATION, Message.LANGUAGE_TAG_EXISTS))
    } else {
      Optional.empty()
    }
  }
}
