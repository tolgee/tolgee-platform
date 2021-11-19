package io.tolgee.dtos.request.validators.exceptions

import io.tolgee.constants.Message
import io.tolgee.dtos.request.validators.ValidationError
import io.tolgee.dtos.request.validators.ValidationErrorType

class ValidationException : RuntimeException {
  val validationErrors: MutableSet<ValidationError> = LinkedHashSet()

  constructor(message: Message, vararg parameters: String) {
    validationErrors.add(ValidationError(ValidationErrorType.CUSTOM_VALIDATION, message, *parameters))
  }

  constructor(validationErrors: Collection<ValidationError>?) {
    this.validationErrors.addAll(validationErrors!!)
  }
}
