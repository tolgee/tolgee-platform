package io.tolgee.dtos.request.validators

import io.tolgee.constants.Message

class ValidationError(
  type: ValidationErrorType,
  message: Message,
  vararg parameters: String,
) {
  val parameters: Array<out String> = parameters
  val type: ValidationErrorType = type
  val message: Message = message
}
