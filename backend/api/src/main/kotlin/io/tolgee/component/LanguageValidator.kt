package io.tolgee.component

import io.tolgee.constants.Message
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.validators.ValidationError
import io.tolgee.dtos.request.validators.ValidationErrorType
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.model.Project
import io.tolgee.service.language.LanguageService
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class LanguageValidator(
  private val languageService: LanguageService,
) {
  fun validateEdit(
    id: Long,
    dto: LanguageRequest,
  ) {
    val validationErrors = LinkedHashSet<ValidationError>()

    // handle edit validation
    val language = languageService.getEntity(id)
    val project = language.project
    if (language.name != dto.name) {
      validateNameUniqueness(dto, project).ifPresent { e: ValidationError -> validationErrors.add(e) }
    }
    if (language.tag != dto.tag) {
      validateTagUniqueness(dto, project).ifPresent { e: ValidationError -> validationErrors.add(e) }
    }
    if (validationErrors.isNotEmpty()) {
      throw ValidationException(validationErrors)
    }
  }

  fun validateCreate(
    dto: LanguageRequest,
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
    dto: LanguageRequest,
    project: Project?,
  ): Optional<ValidationError> {
    return if (languageService.findByName(dto.name, project!!).isPresent) {
      Optional.of(ValidationError(ValidationErrorType.CUSTOM_VALIDATION, Message.LANGUAGE_NAME_EXISTS))
    } else {
      Optional.empty()
    }
  }

  private fun validateTagUniqueness(
    dto: LanguageRequest,
    project: Project?,
  ): Optional<ValidationError> {
    return if (languageService.findByTag(dto.tag, project!!) != null) {
      Optional.of(ValidationError(ValidationErrorType.CUSTOM_VALIDATION, Message.LANGUAGE_TAG_EXISTS))
    } else {
      Optional.empty()
    }
  }
}
