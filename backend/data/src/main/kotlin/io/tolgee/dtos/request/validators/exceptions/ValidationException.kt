package io.tolgee.dtos.request.validators.exceptions

import io.tolgee.constants.MessageInterface
import io.tolgee.dtos.request.validators.ValidationError
import io.tolgee.dtos.request.validators.ValidationErrorType
import io.tolgee.exceptions.ExpectedException

class ValidationException :
  RuntimeException,
  ExpectedException {
  val validationErrors: MutableSet<ValidationError> = LinkedHashSet()

  constructor(message: MessageInterface, vararg parameters: String) {
    validationErrors.add(ValidationError(ValidationErrorType.CUSTOM_VALIDATION, message, *parameters))
  }

  constructor(validationErrors: Collection<ValidationError>?) {
    this.validationErrors.addAll(validationErrors!!)
  }
}
